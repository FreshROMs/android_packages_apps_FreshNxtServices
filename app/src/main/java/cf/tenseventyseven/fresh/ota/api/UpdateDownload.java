package cf.tenseventyseven.fresh.ota.api;

import android.content.Context;
import android.content.Intent;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Func;

import cf.tenseventyseven.fresh.ota.SoftwareUpdate;
import cf.tenseventyseven.fresh.ota.UpdateUtils;
import cf.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;

public class UpdateDownload {
    private static final String FETCH_INSTANCE_NAME = "UpdateDownload";
    private static volatile UpdateDownload instance;
    private static boolean isForeground = false;
    private static Fetch fetch;

    public UpdateDownload() {
        if (instance != null) {
            throw new RuntimeException("Uh-oh! Use getFetchInstance() method to get the single instance of UpdateDownload");
        }
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
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

    public static void setIsForeground(Boolean bool) {
        if (instance != null)
            isForeground = bool;
    }

    public static boolean getIsForeground() {
        return instance != null && isForeground;
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

        // Delete all past instances to avoid re-downloading old, failed or pending downloads.
        fetch.deleteAll();

        UpdateUtils.deleteUpdatePackageFile();
        final Request request = new Request(update.getFileUrl(), UpdateUtils.getUpdatePackageFile().getPath());

        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        CurrentSoftwareUpdate.setOtaDownloadId(context, request.getId());

        fetch.enqueue(request, success, error);
    }

    public static void startService(Context context) {
        try {
            if (!UpdateDownloadService.isAvailable())
                context.startForegroundService(new Intent(context, UpdateDownloadService.class));
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
