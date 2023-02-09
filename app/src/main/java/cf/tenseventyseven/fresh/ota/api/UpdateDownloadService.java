package cf.tenseventyseven.fresh.ota.api;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
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

import cf.tenseventyseven.fresh.R;
import cf.tenseventyseven.fresh.ota.SoftwareUpdate;
import cf.tenseventyseven.fresh.ota.UpdateNotifications;
import cf.tenseventyseven.fresh.ota.UpdateUtils;
import cf.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import cf.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import cf.tenseventyseven.fresh.ota.receivers.DownloadCancelReceiver;
import cf.tenseventyseven.fresh.ota.receivers.DownloadPauseReceiver;
import cf.tenseventyseven.fresh.ota.receivers.DownloadResumeReceiver;
import cf.tenseventyseven.fresh.utils.Notifications;

public class UpdateDownloadService extends Service {
    public static UpdateDownloadService INSTANCE = null;
    public static final String FETCH_GROUP_ID = "fetch_group_id";

    private Fetch fetch;
    private FetchListener fetchListener;
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
        startForeground(UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID,
                UpdateNotifications.getOngoingDownloadNotification(INSTANCE),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        notificationManager = Notifications.getNotificationManager(this);
        fetch = UpdateDownload.getFetchInstance(this);
        fetchListener = new FetchListener() {
            @Override
            public void onResumed(@NonNull Download download) {
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING);
                syncService(download, true);
            }

            @Override
            public void onRemoved(@NonNull Download download) {
                syncService(download, false);
            }

            @Override
            public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {
                syncService(download, true);
            }

            @Override
            public void onDeleted(@NonNull Download download) {
                syncService(download, false);
            }

            @Override
            public void onAdded(@NonNull Download download) {
                syncService(download, true);
            }

            @Override
            public void onWaitingNetwork(@NonNull Download download) {
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_LOST_CONNECTION);
                syncService(download, true);
            }

            @Override
            public void onCancelled(@NonNull Download download) {
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_CANCELLED);
                syncService(download, false);
            }

            @Override
            public void onCompleted(@NonNull Download download) {
                syncService(download, false);

                Notifications.cancelNotification(INSTANCE, UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID);
                if (!UpdateDownload.getIsForeground())
                    UpdateDownload.tryStopService(INSTANCE);
            }

            @Override
            public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_FAILED);
                syncService(download, false);
            }

            @Override
            public void onProgress(@NonNull Download download, long l, long l1) {
                syncService(download, false);
            }

            @Override
            public void onQueued(@NonNull Download download, boolean waiting) {
                syncService(download, waiting);
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING);
            }

            @Override
            public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {
                syncService(download, false);
            }

            @Override
            public void onPaused(@NotNull Download download) {
                CurrentSoftwareUpdate.setOtaState(INSTANCE, SoftwareUpdate.OTA_INSTALL_STATE_PAUSED);

                // Save current progress to db
                CurrentSoftwareUpdate.setOtaDownloadProgress(INSTANCE, download.getProgress());
                CurrentSoftwareUpdate.setOtaDownloadEta(INSTANCE, download.getEtaInMilliSeconds());

                syncService(download, false);
            }
        };

        fetch.addListener(fetchListener);
    }

    @SuppressLint("SetWorldReadable")
    public void verifyUpdate() {
        CurrentSoftwareUpdate.setOtaState(this, SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING);

        File file = UpdateUtils.getUpdatePackageFile();

        int state = 0;

        if (!file.exists())
            state = SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION;

        SystemClock.sleep(4500);

        if (state != SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION) {
            if (UpdateUtils.verifyPackage(this))
                state = SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED;
            else
                state = SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION;
        }

        CurrentSoftwareUpdate.setOtaState(this, state);
        if (state == SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED) {
            //noinspection ResultOfMethodCallIgnored
            file.setReadable(true, false);
            UpdateNotifications.showPreUpdateNotification(this);
        } else {
            UpdateNotifications.showFailedVerificationNotification(this);
        }
    }

    private void syncService(Download download, boolean waitingNetwork) {
        int groupId = download.getId();
        final Status status = download.getStatus();
        final SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(this);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, UpdateNotifications.NOTIFICATION_ONGOING_CHANNEL_ID);

        builder.setContentTitle(update.getReleaseName());
        builder.setSmallIcon(R.drawable.ic_notification_software_update);
        builder.setColor(ContextCompat.getColor(this, R.color.fresh_ic_launcher_background));
        builder.setWhen(download.getCreated());
        builder.setContentIntent(getAvailableActivity());
        builder.setAutoCancel(false);

        final NotificationCompat.BigTextStyle progressBigText = new NotificationCompat.BigTextStyle();

        if (status == Status.COMPLETED) {
            builder.setProgress(100, 0, true);
            builder.setContentTitle(getString(R.string.fresh_ota_main_title));
            builder.setOngoing(true);
            builder.setStyle(progressBigText);
            progressBigText.bigText(getString(R.string.fresh_ota_verifying_update));
            notificationManager.notify(UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID, builder.build());

            if (CurrentSoftwareUpdate.getOtaState(this) == SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING) {
                verifyUpdate();
                notificationManager.cancel(UpdateNotifications.NOTIFICATION_DOWNLOADING_UPDATE_ID);
            }
            return;
        }

        if (CurrentSoftwareUpdate.getOtaState(this) == SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING)
            return;

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
            case QUEUED:
                builder.setContentTitle(getString(R.string.fresh_ota_main_title));
                builder.setContentText(waitingNetwork ? getString(R.string.fresh_ota_changelog_appbar_waiting) : getString(R.string.fresh_ota_changelog_appbar_downloading));
                builder.setOngoing(true);
                break;
            default:
                builder.setContentText(waitingNetwork ? getString(R.string.fresh_ota_changelog_appbar_waiting) : getString(R.string.fresh_ota_changelog_appbar_downloading));
                builder.setOngoing(true);
                break;
        }

        final int progress = download.getProgress();

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