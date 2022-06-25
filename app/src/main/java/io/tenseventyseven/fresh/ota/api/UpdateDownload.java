package io.tenseventyseven.fresh.ota.api;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Func;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Experience;

public class UpdateDownload {
    private static final String FETCH_INSTANCE_NAME = "UpdateDownload";
    private static volatile UpdateDownload instance;
    private static Fetch fetch;

    // OTA download state
    public static final int OTA_DOWNLOAD_STATE_NOT_STARTED = -1;
    public static final int OTA_DOWNLOAD_STATE_COMPLETE = 1;
    public static final int OTA_DOWNLOAD_STATE_DOWNLOADING = 2;
    public static final int OTA_DOWNLOAD_STATE_FAILED = 3;
    public static final int OTA_DOWNLOAD_STATE_PAUSED = 4;
    public static final int OTA_DOWNLOAD_STATE_CANCELLED = 5;
    public static final int OTA_DOWNLOAD_STATE_VERIFYING = 6;
    public static final int OTA_DOWNLOAD_STATE_FAILED_VERIFICATION = 7;
    public static final int OTA_DOWNLOAD_STATE_UNKNOWN = 8;

    public UpdateDownload() {
        if (instance != null) {
            throw new RuntimeException("Uh-oh! Use getFetchInstance() method to get the single instance of UpdateDownload");
        }
    }

    public static Fetch getFetchInstance(Context context) {
        if (instance == null) {
            synchronized (UpdateDownload.class) {
                if (instance == null) {
                    instance = new UpdateDownload();
                    fetch = getFetch(context);
                }
            }
        }
        return fetch;
    }

    public static Fetch getFetch(Context context) {
        FetchConfiguration.Builder fc = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(1)
                .setAutoRetryMaxAttempts(3)
                .setNamespace(FETCH_INSTANCE_NAME)
                .setProgressReportingInterval(500)
                .enableRetryOnNetworkGain(true)
                .enableAutoStart(true)
                .enableHashCheck(true);

        return Fetch.Impl.getInstance(fc.build());
    }

    public static void downloadUpdate(Context context, Func<Request> success, Func<Error> error) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
        Fetch fetch = getFetchInstance(context);
        final Request request = new Request(update.getFileUrl(), UpdateUtils.getUpdatePackageFile().getPath());

        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        CurrentSoftwareUpdate.setOtaDownloadId(context, request.getId());

        fetch.enqueue(request, success, error);
    }

    public static void startService(Context context) {
        try {
            if (!UpdateDownloadService.isAvailable())
                context.startService(new Intent(context, UpdateDownloadService.class));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void tryStopService(Context context) {
        try {
            if (UpdateDownloadService.isAvailable()) {
                context.stopService(new Intent(context, UpdateDownloadService.class));
                instance = null;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
