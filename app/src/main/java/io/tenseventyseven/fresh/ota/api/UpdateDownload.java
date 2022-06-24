package io.tenseventyseven.fresh.ota.api;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Experience;

public class UpdateDownload {
    private static final String FETCH_INSTANCE_NAME = "UpdateDownload";

    public static final int DOWNLOAD_POSSIBLE = 0;
    public static final int NOT_POSSIBLE_NO_CONNECTION = 1;
    public static final int NOT_POSSIBLE_UNREACHABLE = 2;
    public static final int NOT_POSSIBLE_METERED = 2;
    public static final int NOT_POSSIBLE_UNKNOWN = -1;

    private static final long UPDATE_STATUS_INTERVAL = 1000;

    public static FetchConfiguration getFetchConfig(Context context) {
        return new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(1)
                .setAutoRetryMaxAttempts(3)
                .enableLogging(true)
                .setNamespace(FETCH_INSTANCE_NAME)
                .setProgressReportingInterval(500)
                .enableHashCheck(true)
                .build();
    }

    public static int isDownloadPossible(Context context, String downloadUrl) {
        // Just bail out immediately if there's no internet connection.
        if (!UpdateUtils.isDeviceOnline(context))
            return NOT_POSSIBLE_NO_CONNECTION;

        if (!UpdateUtils.isConnectionUnmetered(context))
            return NOT_POSSIBLE_METERED;

        // Test connection to the server
        try {
            URL url = new URL(downloadUrl);
            URLConnection  connection = url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.connect();
            return DOWNLOAD_POSSIBLE;
        } catch (IOException e) {
            e.printStackTrace();
            return NOT_POSSIBLE_UNREACHABLE;
        }
    }
}
