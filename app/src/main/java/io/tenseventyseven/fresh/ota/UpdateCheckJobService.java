package io.tenseventyseven.fresh.ota;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONException;
import org.lineageos.updater.download.DownloadClient;

import java.io.File;
import java.io.IOException;

import io.tenseventyseven.fresh.ota.api.UpdateManifest;

public class UpdateCheckJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(new Runnable() {
            final Context context = UpdateCheckJobService.this;

            @Override
            public void run() {
                String server = UpdateManifest.whichServiceReachable(context);

                // Reschedule if no server is reachable
                if (server == null) {
                    jobFinished(params, true);
                    return;
                }

                // Bail immediately if there's a pending update
                if (UpdateManifest.getUpdateAvailability(context)) {
                    UpdateNotifications.showNewUpdateNotification(context);
                    jobFinished(params, true);
                    return;
                }

                UpdateNotifications.showOngoingCheckNotification(context);
                final File json = new File(context.getFilesDir(), UpdateManifest.MANIFEST_FILE_NAME);

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
                            if (!json.exists() || !UpdateManifest.parseManifest(context, json) || UpdateManifest.getUpdateAvailability(context)) {
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
                    if (UpdateManifest.getUpdateAvailability(context))
                        UpdateNotifications.showNewUpdateNotification(context);

                    UpdateNotifications.cancelOngoingCheckNotification(context);
                    Settings.System.putInt(context.getContentResolver(), "badge_for_fota", UpdateManifest.getUpdateAvailability(context) ? 1 : 0);
                    Settings.System.putLong(context.getContentResolver(), "SOFTWARE_UPDATE_LAST_CHECKED_DATE", System.currentTimeMillis());
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
