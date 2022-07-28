package io.tenseventyseven.fresh.ota.api;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import io.tenseventyseven.fresh.ota.UpdateUtils;

public class UpdateInstallService extends JobService {
    public UpdateInstallService() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Context context = this;
        new Thread(() -> UpdateUtils.installUpdate(context));
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void setupInstallJob(Context context) {
        ComponentName serviceName = new ComponentName(context, UpdateCheckService.class);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(UpdateUtils.JOB_INSTALL_LATER_ID, serviceName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setRequiresStorageNotLow(true)
                .setRequiresDeviceIdle(true)
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(true)
                .build();

        jobScheduler.schedule(jobInfo);
    }

    public static void cancelInstallJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler == null)
            return;

        jobScheduler.cancel(UpdateUtils.JOB_INSTALL_LATER_ID);
    }
}