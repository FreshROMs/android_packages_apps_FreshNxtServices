package io.tenseventyseven.fresh.ota.api;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.lineageos.updater.download.DownloadClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.tenseventyseven.fresh.ota.UpdateNotifications;
import io.tenseventyseven.fresh.ota.UpdateUtils;

public class UpdateCheckJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(new Runnable() {
            final Context context = UpdateCheckJobService.this;

            @Override
            public void run() {
                String server = UpdateCheck.whichServiceReachable(context);

                // Reschedule if no server is reachable
                if (server == null) {
                    jobFinished(params, true);
                    return;
                }

                // Bail immediately if there's a pending update
                if (UpdateCheck.getUpdateAvailability(context)) {
                    UpdateNotifications.showNewUpdateNotification(context);
                    jobFinished(params, true);
                    return;
                }

                UpdateNotifications.showOngoingCheckNotification(context);
                final File json = new File(context.getFilesDir(), UpdateCheck.MANIFEST_FILE_NAME);

                DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
                    @Override
                    public void onFailure(boolean cancelled) {
                        jobFinished(params, true);
                    }

                    @Override
                    public void onResponse(DownloadClient.Headers headers) {
                    }

                    @Override
                    public void onSuccess() {
                        try {
                            if (!json.exists() || !UpdateCheck.parseManifest(context, json) || UpdateCheck.getUpdateAvailability(context)) {
                                jobFinished(params, true);
                                return;
                            }

                            jobFinished(params, false);
                        } catch (IOException | JSONException e) {
                            jobFinished(params, true);
                        }
                    }
                };

                try {
                    DownloadClient downloadClient = new DownloadClient.Builder()
                            .setUrl(server)
                            .setDestination(json)
                            .setDownloadCallback(callback)
                            .build();
                    downloadClient.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    jobFinished(params, true);
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            final Context context = UpdateCheckJobService.this;

            @Override
            public void run() {
                handler.postDelayed(() -> {
                    if (UpdateCheck.getUpdateAvailability(context)) {
                        if (UpdateUtils.isWlanAutoDownload(context)) {
                            UpdateDownload.startService(context);
                            UpdateDownload.downloadUpdate(context,  success -> {}, fail -> {
                                // Show a notification instead if we fail
                                UpdateNotifications.showNewUpdateNotification(context);
                                UpdateDownload.tryStopService(context);
                            });
                        } else {
                            UpdateNotifications.showNewUpdateNotification(context);
                        }
                    }

                    UpdateNotifications.cancelOngoingCheckNotification(context);
                    UpdateUtils.setSettingAppBadge(context, UpdateCheck.getUpdateAvailability(context));
                    UpdateUtils.setLastCheckedDate(context);
                }, 2000);
            }
        }).start();

        return true;
    }

    public static void setupCheckJob(Context context) {
        ComponentName serviceName = new ComponentName(context, UpdateCheckJobService.class);
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
