package io.tenseventyseven.fresh.ota;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;
import io.tenseventyseven.fresh.ota.activity.LastUpdateActivity;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Notifications;

public class UpdateNotifications {
    public static String NOTIFICATION_GROUP_ID = "tns_fresh_notification_group_ota";
    public static String NOTIFICATION_CHANNEL_ID = "tns_fresh_notification_channel_ota";
    public static String NOTIFICATION_CHANNEL_APP_ID = "tns_fresh_notification_channel_app";
    public static String NOTIFICATION_ONGOING_CHANNEL_ID = "tns_fresh_notification_channel_ongoing_ota";

    public static int NOTIFICATION_CHECK_UPDATE_ID = 1077500;
    public static int NOTIFICATION_AVAILABLE_UPDATE_ID = 1077501;
    public static int NOTIFICATION_POST_UPDATE_ID = 1077502;
    public static int NOTIFICATION_DOWNLOADING_UPDATE_ID = 1077503;
    public static int NOTIFICATION_VERIFY_UPDATE_ID = 1077504;

    public static Notification getOngoingCheckNotification(Context context) {
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_ONGOING_CHANNEL_ID, UpdateCheckActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_main_title);
        String notificationContent = context.getString(R.string.fresh_ota_checking_for_updates);

        return builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    public static Notification getOngoingDownloadNotification(Context context) {
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_ONGOING_CHANNEL_ID, UpdateCheckActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_main_title);
        String notificationContent = context.getString(R.string.fresh_ota_changelog_appbar_downloading);

        return builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    public static void showOngoingCheckNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        Notification notification = getOngoingCheckNotification(context);
        notificationManager.notify(NOTIFICATION_CHECK_UPDATE_ID, notification);
    }

    public static void showOngoingVerificationNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_ONGOING_CHANNEL_ID, UpdateAvailableActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_main_title);
        String notificationContent = context.getString(R.string.fresh_ota_verifying_update);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        notificationManager.notify(NOTIFICATION_VERIFY_UPDATE_ID, notification);
    }

    public static void showFailedVerificationNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_download_failed_title);
        String notificationContent = context.getString(R.string.fresh_ota_notification_verification_failed_description);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_AVAILABLE_UPDATE_ID, notification);
    }

    public static void showNewUpdateNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_available_title);
        String notificationContent = context.getString(R.string.fresh_ota_notification_update_available_description);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_AVAILABLE_UPDATE_ID, notification);
    }

    public static void showPreUpdateNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_install_title);
        String notificationContent = context.getString(R.string.fresh_ota_changelog_appbar_install, update.getVersionName(), update.getFormattedReleaseType());

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_POST_UPDATE_ID, notification);
    }

    public static void showPostUpdateNotification(Context context, boolean success) {
        Class<?> cls = success ? LastUpdateActivity.class : UpdateCheckActivity.class;
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_CHANNEL_ID, cls);
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_failed_title);
        String notificationContent = context.getString(R.string.fresh_ota_notification_update_failed_description,
                update.getFormattedVersion());

        if (success) {
            notificationTitle = context.getString(R.string.fresh_ota_notification_update_success_title);
            notificationContent = context.getString(R.string.fresh_ota_notification_update_success_description,
                    update.getFormattedVersion());
        }

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_POST_UPDATE_ID, notification);
    }

    public static void setupNotificationChannels(Context context) {
        // Setup notifications
        Notifications.setupNotificationGroup(context, NOTIFICATION_GROUP_ID, R.string.fresh_ota_notification_group_name);
        Notifications.setupNotificationChannel(context,
                NOTIFICATION_GROUP_ID, NOTIFICATION_CHANNEL_ID,
                R.string.fresh_ota_notification_channel_name, R.string.fresh_ota_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                NOTIFICATION_GROUP_ID, NOTIFICATION_CHANNEL_APP_ID,
                R.string.fresh_app_notification_channel_name, R.string.fresh_app_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                NOTIFICATION_GROUP_ID, NOTIFICATION_ONGOING_CHANNEL_ID,
                R.string.fresh_ota_ongoing_notification_channel_name, R.string.fresh_ota_ongoing_notification_channel_description, NotificationManager.IMPORTANCE_LOW);

    }

    public static void cancelOngoingCheckNotification(Context context) {
        Notifications.cancelNotification(context, NOTIFICATION_CHECK_UPDATE_ID);
    }

    public static void cancelOngoingVerificationNotification(Context context) {
        Notifications.cancelNotification(context, NOTIFICATION_VERIFY_UPDATE_ID);
    }
}
