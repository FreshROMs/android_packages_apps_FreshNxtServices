package io.tenseventyseven.fresh.ota;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.annotation.IntRange;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.DecimalFormat;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;
import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Experience;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Notifications;
import io.tenseventyseven.fresh.utils.Tools;

public class Utils {
    public static String PROP_FRESH_OTA_API = "ro.fresh.ota.api";
    public static String PROP_FRESH_OTA_API_MIRROR = "ro.fresh.ota.api.mirror";
    public static String PROP_FRESH_DEVICE_PRODUCT = "ro.fresh.device.product";
    public static String PROP_FRESH_ROM_VERSION_CODE = "ro.fresh.build.version";

    public static String PROP_FRESH_ROM_BRANCH = SystemProperties.get("ro.fresh.build.branch");
    public static String PROP_FRESH_ROM_VERSION_NAME = SystemProperties.get("ro.fresh.version");
    public static String PROP_FRESH_ROM_VERSION_UTC = SystemProperties.get("ro.fresh.build.date.utc");

    public static String NOTIFICATION_GROUP_ID = "tns_fresh_notification_group_ota";
    public static String NOTIFICATION_CHANNEL_ID = "tns_fresh_notification_channel_ota";
    public static String NOTIFICATION_CHANNEL_APP_ID = "tns_fresh_notification_channel_app";
    public static String NOTIFICATION_ONGOING_CHANNEL_ID = "tns_fresh_notification_channel_ongoing_ota";

    private static final String DAILY_CHECK_ACTION = "daily_check_action";
    private static final String ONESHOT_CHECK_ACTION = "oneshot_check_action";

    public static int NOTIFICATION_CHECK_UPDATE_ID = 1077500;
    public static int NOTIFICATION_AVAILABLE_UPDATE_ID = 1077501;
    public static int NOTIFICATION_POST_UPDATE_ID = 1077502;

    public static boolean getUpdateAvailability(Context context) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
        String current = SystemProperties.get(PROP_FRESH_ROM_VERSION_CODE);
        String manifest = Long.toString(update.getVersionCode());

        if (current.length() > manifest.length()) {
            StringBuilder manifestBuilder = new StringBuilder(manifest);
            for (int i = 0; i < current.length() - manifestBuilder.length(); i++) {
                manifestBuilder.append("0");
            }
            manifest = manifestBuilder.toString();
        } else if (manifest.length() > current.length()) {
            StringBuilder currentBuilder = new StringBuilder(current);
            for (int i = 0; i < manifest.length() - currentBuilder.length(); i++) {
                currentBuilder.append("0");
            }
            current = currentBuilder.toString();
        }

        return Long.parseLong(current) < Long.parseLong(manifest);
    }

    public static File getUpdatePackageFile() {
        return new File(Experience.getFreshDir(), "update.zip");
    }

    public static void showOngoingCheckNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_ONGOING_CHANNEL_ID, UpdateCheckActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_main_title);
        String notificationContent = context.getString(R.string.fresh_ota_checking_for_updates);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(context.getResources().getColor(R.color.primary_color))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        notificationManager.notify(NOTIFICATION_CHECK_UPDATE_ID, notification);
    }

    public static void showNewUpdateNotification(Context context) {
        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        NotificationCompat.Builder builder = Notifications.getNotificationBuilder(context, NOTIFICATION_CHANNEL_ID, UpdateAvailableActivity.class);

        String notificationTitle = context.getString(R.string.fresh_ota_notification_update_available_title);
        String notificationContent = context.getString(R.string.fresh_ota_notification_update_available_description);

        Notification notification = builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(context.getResources().getColor(R.color.primary_color))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_AVAILABLE_UPDATE_ID, notification);
    }

    public static void showPostUpdateNotification(Context context, boolean success) {
        Class<?> cls = success ? LastSoftwareUpdate.class : UpdateCheckActivity.class;
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
                .setColor(context.getResources().getColor(R.color.primary_color))
                .setSmallIcon(R.drawable.ic_notification_software_update)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build();

        notificationManager.notify(NOTIFICATION_POST_UPDATE_ID, notification);
    }

    public static void setupNotificationChannels(Context context) {
        // Setup notifications
        Notifications.setupNotificationGroup(context, Utils.NOTIFICATION_GROUP_ID, R.string.fresh_ota_notification_group_name);
        Notifications.setupNotificationChannel(context,
                Utils.NOTIFICATION_GROUP_ID, Utils.NOTIFICATION_CHANNEL_ID,
                R.string.fresh_ota_notification_channel_name, R.string.fresh_ota_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                Utils.NOTIFICATION_GROUP_ID, Utils.NOTIFICATION_CHANNEL_APP_ID,
                R.string.fresh_app_notification_channel_name, R.string.fresh_app_notification_channel_description, NotificationManager.IMPORTANCE_HIGH);
        Notifications.setupNotificationChannel(context,
                Utils.NOTIFICATION_GROUP_ID, Utils.NOTIFICATION_ONGOING_CHANNEL_ID,
                R.string.fresh_ota_ongoing_notification_channel_name, R.string.fresh_ota_ongoing_notification_channel_description, NotificationManager.IMPORTANCE_LOW);

    }

    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) return false;

        return  capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    public static String getFormattedFileSize(long fileSize) {
        if (fileSize <= 0) return "0B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
        int digitGroups = (int) (Math.log10(fileSize)/Math.log10(1000));
        return new DecimalFormat("#,##0.#").format(fileSize/Math.pow(1000, digitGroups)) + " " + units[digitGroups];
    }

    public static void cleanupDownloadsDir() {
        File otaFile = new File(Experience.getFreshDir(), "update.zip");
        if (otaFile.exists())
            otaFile.delete();
    }

    public static void cancelOngoingCheckNotification(Context context) {
        Notifications.cancelOngoingNotification(context, NOTIFICATION_CHECK_UPDATE_ID);
    }

    public static PendingIntent getRepeatingUpdatesCheckIntent(Context context) {
        Intent intent = new Intent(context, UpdateCheckReceiver.class);
        intent.setAction(DAILY_CHECK_ACTION);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateRepeatingUpdatesCheck(Context context) {
        cancelRepeatingUpdatesCheck(context);
        scheduleRepeatingUpdatesCheck(context);
    }

    public static void scheduleRepeatingUpdatesCheck(Context context) {
        PendingIntent updateCheckIntent = getRepeatingUpdatesCheckIntent(context);
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        alarmMgr.setRepeating(AlarmManager.RTC, System.currentTimeMillis() +
                        (AlarmManager.INTERVAL_HOUR * 4), (AlarmManager.INTERVAL_HOUR * 4),
                updateCheckIntent);
    }

    public static void cancelRepeatingUpdatesCheck(Context context) {
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        alarmMgr.cancel(getRepeatingUpdatesCheckIntent(context));
    }

    public static PendingIntent getUpdatesCheckIntent(Context context) {
        Intent intent = new Intent(context, UpdateCheckReceiver.class);
        intent.setAction(ONESHOT_CHECK_ACTION);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public static void scheduleUpdatesCheck(Context context) {
        long millisToNextCheck = AlarmManager.INTERVAL_HOUR * 4;
        PendingIntent updateCheckIntent = getUpdatesCheckIntent(context);
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + millisToNextCheck,
                updateCheckIntent);
    }

    public static void cancelUpdatesCheck(Context context) {
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        alarmMgr.cancel(getUpdatesCheckIntent(context));
    }
}
