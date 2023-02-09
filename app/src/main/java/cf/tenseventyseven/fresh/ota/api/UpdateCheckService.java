package cf.tenseventyseven.fresh.ota.api;

import android.app.Service;
import android.app.job.JobParameters;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import cf.tenseventyseven.fresh.ota.UpdateNotifications;

public class UpdateCheckService extends Service {
    public static UpdateCheckService INSTANCE = null;
    public static JobParameters mParams = null;

    private Fetch fetch;
    private AbstractFetchGroupListener fetchListener;
    private PowerManager.WakeLock mWakeLock;

    public static boolean isAvailable() {
        try {
            return INSTANCE != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;

        PowerManager powerManager = getSystemService(PowerManager.class);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UpdateCheckService:wakelock");
        mWakeLock.setReferenceCounted(false);

        UpdateNotifications.setupNotificationChannels(this);
        startForeground(UpdateNotifications.NOTIFICATION_CHECK_UPDATE_ID,
                UpdateNotifications.getOngoingCheckNotification(INSTANCE),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        fetch = UpdateCheck.getFetchInstance(this);
        fetchListener = new AbstractFetchGroupListener() {
            @Override
            public void onCancelled(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> UpdateCheck.finishCheck(INSTANCE, false, false), 2000);
            }

            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                File json = new File(download.getFile());
                Handler handler = new Handler(Looper.getMainLooper());
                try {
                    if (!json.exists() || !UpdateCheck.parseManifest(INSTANCE, json)) {
                        handler.postDelayed(() -> UpdateCheck.finishCheck(INSTANCE, false, false), 2000);
                        return;
                    }

                    handler.postDelayed(() -> UpdateCheck.finishCheck(INSTANCE, UpdateCheck.getUpdateAvailability(INSTANCE), true), 2000);
                } catch (IOException | JSONException e) {
                    handler.postDelayed(() -> UpdateCheck.finishCheck(INSTANCE, false, false), 2000);
                }
            }

            @Override
            public void onError(int groupId, @NonNull Download download, @NonNull Error error, @Nullable Throwable throwable, FetchGroup fetchGroup) {
            }

            @Override
            public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
            }

            @Override
            public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                UpdateNotifications.showOngoingCheckNotification(INSTANCE);
            }

            @Override
            public void onPaused(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
            }
        };

        fetch.addListener(fetchListener);
    }

    @Override
    public void onDestroy() {
        fetch.removeListener(fetchListener);
        fetch.close();
        fetch = null;
        INSTANCE = null;

        if (mWakeLock != null)
            mWakeLock.release();

        super.onDestroy();
    }
}
