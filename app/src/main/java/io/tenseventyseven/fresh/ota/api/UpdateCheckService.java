package io.tenseventyseven.fresh.ota.api;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
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

import io.tenseventyseven.fresh.ota.UpdateNotifications;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;

public class UpdateCheckService extends JobService {
    public static UpdateCheckService INSTANCE = null;
    public static Context mContext = null;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;
        mContext = this;

        PowerManager powerManager = getSystemService(PowerManager.class);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UpdateCheckService:wakelock");
        mWakeLock.setReferenceCounted(false);

        UpdateNotifications.setupNotificationChannels(this);

        fetch = UpdateCheck.getFetchInstance(this);
        fetchListener = new AbstractFetchGroupListener() {
            @Override
            public void onCancelled(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                finishJob(mParams, true);
            }

            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                File json = new File(download.getFile());
                Handler handler = new Handler(Looper.getMainLooper());
                try {
                    if (!json.exists() || !UpdateCheck.parseManifest(INSTANCE, json)) {
                        finishJob(mParams, true);
                        return;
                    }

                    boolean updateAvailable = UpdateCheck.getUpdateAvailability(mContext);
                    handler.postDelayed(() -> {

                        if (updateAvailable)
                            UpdateNotifications.showNewUpdateNotification(mContext);

                        UpdateNotifications.cancelOngoingCheckNotification(mContext);
                        UpdateUtils.setSettingAppBadge(mContext, updateAvailable);
                        UpdateUtils.setLastCheckedDate(mContext);
                    }, 2000);

                    finishJob(mParams, updateAvailable);
                } catch (IOException | JSONException e) {
                    finishJob(mParams, true);
                    handler.postDelayed(() -> {

                        UpdateNotifications.cancelOngoingCheckNotification(mContext);
                        UpdateUtils.setSettingAppBadge(mContext, false);
                        UpdateUtils.setLastCheckedDate(mContext);
                    }, 2000);
                }
            }

            @Override
            public void onError(int groupId, @NonNull Download download, @NonNull Error error, @Nullable Throwable throwable, FetchGroup fetchGroup) {
                finishJob(mParams, true);
            }

            @Override
            public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
            }

            @Override
            public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                UpdateNotifications.showOngoingCheckNotification(mContext);
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

    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;

        new Thread(new Runnable() {
            final Context context = UpdateCheckService.this;

            @Override
            public void run() {
                // Bail immediately if there's a pending install
                if (CurrentSoftwareUpdate.getOtaDownloadState(context) == UpdateDownload.OTA_DOWNLOAD_STATE_COMPLETE) {
                    UpdateNotifications.showPreUpdateNotification(context);
                    jobFinished(params, true);
                    return;
                }

                // Bail immediately if there's a pending update
                if (UpdateCheck.getUpdateAvailability(context)) {
                    UpdateNotifications.showNewUpdateNotification(context);
                    jobFinished(params, true);
                    return;
                }

                UpdateCheck.startService(context);

                UpdateCheck.downloadManifest(context, success -> {
                }, error -> finishJob(params, true));
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        new Thread(new Runnable() {
            final Context context = UpdateCheckService.this;

            @Override
            public void run() {
                UpdateCheck.tryStopService(context);
            }
        }).start();

        return true;
    }

    private void finishJob(JobParameters params, boolean wantsReschedule) {
        if (params != null)
            jobFinished(params, wantsReschedule);
    }

    public static void setupCheckJob(Context context) {
        ComponentName serviceName = new ComponentName(context, UpdateCheckService.class);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        int jobInterval = 14400; // Every 4 hours

        JobInfo jobInfo = new JobInfo.Builder(UpdateUtils.JOB_UPDATE_CHECK_ID, serviceName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresStorageNotLow(true)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setPeriodic(jobInterval * 1000, jobInterval * 500)
                .build();

        jobScheduler.schedule(jobInfo);
    }

    public static void cancelCheckJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler == null)
            return;

        jobScheduler.cancel(UpdateUtils.JOB_UPDATE_CHECK_ID);
    }
}
