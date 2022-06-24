package io.tenseventyseven.fresh.ota;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Notifications;
import io.tenseventyseven.fresh.utils.Tools;

public class UpdateNotifications {
    public static void showOngoingCheckNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, UpdateUtils.NOTIFICATION_ONGOING_CHANNEL_ID, UpdateCheckActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_main_title);
        String notificationContent = context.getString(R.string.fresh_ota_checking_for_updates);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        notificationManager.notify(UpdateUtils.NOTIFICATION_CHECK_UPDATE_ID, notification);
    }

    public static void showNewUpdateNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, UpdateUtils.NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_available_title);
        String notificationContent = context.getString(R.string.fresh_ota_notification_update_available_description);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(UpdateUtils.NOTIFICATION_AVAILABLE_UPDATE_ID, notification);
    }

    public static void showPreUpdateNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, UpdateUtils.NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_available_title);
        String notificationContent = context.getString(R.string.fresh_ota_changelog_appbar_install, update.getVersionName(), Tools.capitalizeString(update.getReleaseType()));

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.fresh_ic_launcher_background))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(UpdateUtils.NOTIFICATION_POST_UPDATE_ID, notification);
    }

    public static void showPostUpdateNotification(Context context, boolean success) {
        Class<?> cls = success ? LastSoftwareUpdate.class : UpdateCheckActivity.class;
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, UpdateUtils.NOTIFICATION_CHANNEL_ID, cls);
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

        notificationManager.notify(UpdateUtils.NOTIFICATION_POST_UPDATE_ID, notification);
    }

    public static void setupNotificationChannels(Context context) {
        // Setup notifications
        Notifications.setupNotificationGroup(context, UpdateUtils.NOTIFICATION_GROUP_ID, R.string.fresh_ota_notification_group_name);
        Notifications.setupNotificationChannel(context,
                UpdateUtils.NOTIFICATION_GROUP_ID, UpdateUtils.NOTIFICATION_CHANNEL_ID,
                R.string.fresh_ota_notification_channel_name, R.string.fresh_ota_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                UpdateUtils.NOTIFICATION_GROUP_ID, UpdateUtils.NOTIFICATION_CHANNEL_APP_ID,
                R.string.fresh_app_notification_channel_name, R.string.fresh_app_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                UpdateUtils.NOTIFICATION_GROUP_ID, UpdateUtils.NOTIFICATION_ONGOING_CHANNEL_ID,
                R.string.fresh_ota_ongoing_notification_channel_name, R.string.fresh_ota_ongoing_notification_channel_description, NotificationManager.IMPORTANCE_LOW);

    }

    public static void cancelOngoingCheckNotification(Context context) {
        Notifications.cancelOngoingNotification(context, UpdateUtils.NOTIFICATION_CHECK_UPDATE_ID);
    }
}
