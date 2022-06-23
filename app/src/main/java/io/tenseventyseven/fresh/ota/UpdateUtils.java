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

public class UpdateUtils {
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

    public static int JOB_UPDATE_CHECK_ID = 1077601;

    public static File getUpdatePackageFile() {
        return new File(Experience.getFreshDir(), "update.zip");
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

    public static void setLastCheckedDate(Context context) {
        Settings.System.putLong(context.getContentResolver(), "SOFTWARE_UPDATE_LAST_CHECKED_DATE", System.currentTimeMillis());
    }

    public static void setSettingAppBadge(Context context, boolean isUpdateAvailable) {
        Settings.System.putInt(context.getContentResolver(), "badge_for_fota", isUpdateAvailable ? 1 : 0);
    }
}
