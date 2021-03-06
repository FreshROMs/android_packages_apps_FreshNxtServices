package io.tenseventyseven.fresh.ota.api;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.UpdateNotifications;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.receivers.DownloadCancelReceiver;
import io.tenseventyseven.fresh.ota.receivers.DownloadPauseReceiver;
import io.tenseventyseven.fresh.ota.receivers.DownloadResumeReceiver;
import io.tenseventyseven.fresh.utils.Notifications;

public class UpdateDownloadService extends Service {
    public static UpdateDownloadService INSTANCE = null;
    public static final String FETCH_GROUP_ID = "fetch_group_id";

    private Fetch fetch;
    private AbstractFetchGroupListener fetchListener;
    private NotificationManager notificationManager;
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
        super.onCreate();
        INSTANCE = this;

        PowerManager powerManager = getSystemService(PowerManager.class);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UpdateDownloadService:wakelock");
        mWakeLock.setReferenceCounted(false);

        UpdateNotifications.setupNotificationChannels(this);

        notificationManager = Notifications.getNotificationManager(this);
        fetch = UpdateDownload.getFetchInstance(this);
        fetchListener = new AbstractFetchGroupListener() {
            @Override
            public void onWaitingNetwork(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_LOST_CONNECTION);
                updateNotification(groupId, download, fetchGroup, true);
            }

            @Override
            public void onCancelled(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_CANCELLED);
                updateNotification(groupId, download, fetchGroup, false);
            }

            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                Notifications.cancelNotification(INSTANCE, UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID);
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_VERIFYING);
                verifyUpdate();
            }

            @Override
            public void onError(int groupId, @NonNull Download download, @NonNull Error error, @Nullable Throwable throwable, FetchGroup fetchGroup) {
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_FAILED);
                updateNotification(groupId, download, fetchGroup, false);
            }

            @Override
            public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
                updateNotification(groupId, download, fetchGroup, false);
            }

            @Override
            public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_DOWNLOADING);
                updateNotification(groupId, download, fetchGroup, false);
            }

            @Override
            public void onStarted(int groupId, @NonNull Download download, @NonNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks, @NonNull FetchGroup fetchGroup) {
                updateNotification(groupId, download, fetchGroup, false);
            }

            @Override
            public void onPaused(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                CurrentSoftwareUpdate.setOtaDownloadState(INSTANCE, UpdateDownload.OTA_DOWNLOAD_STATE_PAUSED);

                // Save current progress to db
                CurrentSoftwareUpdate.setOtaDownloadProgress(INSTANCE, download.getProgress());
                CurrentSoftwareUpdate.setOtaDownloadEta(INSTANCE, download.getEtaInMilliSeconds());

                updateNotification(groupId, download, fetchGroup, false);
            }
        };

        fetch.addListener(fetchListener);
    }

    private void updateNotification(int groupId, Download download, FetchGroup fetchGroup, boolean waitingNetwork) {
        final Status status = download.getStatus();
        final SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(this);

        if (status == Status.COMPLETED && fetchGroup.getGroupDownloadProgress() < 100)
            return;

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, UpdateNotifications.NOTIFICATION_ONGOING_CHANNEL_ID);

        builder.setContentTitle(update.getReleaseName());
        builder.setSmallIcon(R.drawable.ic_notification_software_update);
        builder.setColor(ContextCompat.getColor(this, R.color.fresh_ic_launcher_background));
        builder.setWhen(download.getCreated());
        builder.setContentIntent(getAvailableActivity());
        builder.setAutoCancel(false);

        switch (status) {
            case PAUSED:
                builder.setContentText(getString(R.string.fresh_ota_changelog_appbar_paused));
                builder.setOngoing(true);
                break;
            case FAILED:
                builder.setContentText(getString(R.string.fresh_ota_notification_download_failed_title));
                builder.setOngoing(false);
                break;
            case CANCELLED:
                builder.setContentText(getString(R.string.fresh_ota_notification_download_cancelled));
                builder.setOngoing(false);
                break;
            case COMPLETED:
                if (fetchGroup.getGroupDownloadProgress() == 100) {
                    builder.setOngoing(false);
                    return;
                }
                break;
            default:
                builder.setContentText(waitingNetwork ? getString(R.string.fresh_ota_changelog_appbar_waiting) : getString(R.string.fresh_ota_changelog_appbar_downloading));
                builder.setOngoing(true);
                break;
        }

        final int progress = download.getProgress();
        final NotificationCompat.BigTextStyle progressBigText = new NotificationCompat.BigTextStyle();

        // Set Notification data
        switch (status) {
            case QUEUED:
                builder.setProgress(100, 0, true);
                progressBigText.bigText(waitingNetwork ? getString(R.string.fresh_ota_changelog_appbar_waiting) : getString(R.string.fresh_ota_changelog_appbar_downloading));
                builder.setStyle(progressBigText);
                break;

            case DOWNLOADING:
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String timeLeft = String.format(Locale.getDefault(), "%s %s",
                        getString(R.string.fresh_ota_changelog_appbar_downloading_time_left),
                        (download.getEtaInMilliSeconds() == 0) ? "00:00:00" : formatter.format(new Date(download.getEtaInMilliSeconds())));

                final String speedString = UpdateUtils.getFormattedSpeed(download.getDownloadedBytesPerSecond());

                progressBigText.bigText(String.format("%s \u2022 %s", timeLeft, speedString));
                builder.setStyle(progressBigText);

                builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_oui_pause,
                        getString(R.string.fresh_ota_changelog_btn_pause),
                        getPauseIntent(groupId)).build());

                builder.addAction(new NotificationCompat.Action.Builder(R.drawable.oui_tips_card_view_cancel_button,
                        getString(R.string.fresh_ota_changelog_btn_cancel),
                        getCancelIntent(groupId)).build());

                if (progress < 0)
                    builder.setProgress(100, 0, true);
                else
                    builder.setProgress(100, progress, false);
                break;

            case PAUSED:
                progressBigText.bigText(getString(R.string.fresh_ota_changelog_appbar_paused));
                builder.setStyle(progressBigText);
                builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_oui_download,
                        getString(R.string.fresh_ota_changelog_btn_resume),
                        getResumeIntent(groupId)).build());
                break;

            case COMPLETED:
                if (fetchGroup.getGroupDownloadProgress() == 100) {
                    builder.setAutoCancel(true);
                    UpdateNotifications.showPreUpdateNotification(this);
                    return;
                }
                break;
        }

        switch (status) {
            case DOWNLOADING:
                builder.setCategory(Notification.CATEGORY_PROGRESS);
                break;
            case FAILED:
            case CANCELLED:
                builder.setCategory(Notification.CATEGORY_ERROR);
                break;
            default:
                builder.setCategory(Notification.CATEGORY_STATUS);
                break;
        }

        notificationManager.notify(UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID, builder.build());

        if (status == Status.CANCELLED || status == Status.FAILED) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> Notifications.cancelNotification(this, UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID), 3000);
            fetch.remove(CurrentSoftwareUpdate.getOtaDownloadId(this));
            UpdateUtils.deleteUpdatePackageFile();
            UpdateDownload.tryStopService(this);
        }
    }

    @SuppressLint("SetWorldReadable")
    private void verifyUpdate() {
        UpdateNotifications.showOngoingVerificationNotification(INSTANCE);
        File file = UpdateUtils.getUpdatePackageFile();
        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {
            if (file.exists() && UpdateUtils.verifyPackage(this)) {
                //noinspection ResultOfMethodCallIgnored
                file.setReadable(true, false);
                CurrentSoftwareUpdate.setOtaDownloadVerified(this, true);
                CurrentSoftwareUpdate.setOtaDownloadState(this, UpdateDownload.OTA_DOWNLOAD_STATE_COMPLETE);
                UpdateNotifications.showPreUpdateNotification(this);
            } else {
                CurrentSoftwareUpdate.setOtaDownloadVerified(this, false);
                CurrentSoftwareUpdate.setOtaDownloadState(this, UpdateDownload.OTA_DOWNLOAD_STATE_FAILED_VERIFICATION);
                UpdateNotifications.showFailedVerificationNotification(this);
            }

            UpdateNotifications.cancelOngoingVerificationNotification(this);
        }, 7000);
    }

    private PendingIntent getPauseIntent(int groupId) {
        final Intent intent = new Intent(this, DownloadPauseReceiver.class);
        intent.putExtra(FETCH_GROUP_ID, groupId);
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getResumeIntent(int groupId) {
        final Intent intent = new Intent(this, DownloadResumeReceiver.class);
        intent.putExtra(FETCH_GROUP_ID, groupId);
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getCancelIntent(int groupId) {
        final Intent intent = new Intent(this, DownloadCancelReceiver.class);
        intent.putExtra(FETCH_GROUP_ID, groupId);
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getAvailableActivity() {
        final Intent intent = new Intent(this, UpdateAvailableActivity.class);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
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