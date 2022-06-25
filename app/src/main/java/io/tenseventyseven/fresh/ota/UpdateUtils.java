package io.tenseventyseven.fresh.ota;

import android.annotation.SuppressLint;
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
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;
import io.tenseventyseven.fresh.ota.api.UpdateDownloadService;
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

    public static final String SW_UPDATE_FILE_NAME = "update.zip";
    private static final String ONESHOT_CHECK_ACTION = "oneshot_check_action";

    public static int JOB_UPDATE_CHECK_ID = 1077601;

    public static File getUpdatePackageFile() {
        return new File(Experience.getFreshDir(), SW_UPDATE_FILE_NAME);
    }

    public static void deleteUpdatePackageFile() {
        File otaFile = new File(Experience.getFreshDir(), "update.zip");
        if (otaFile.exists())
            otaFile.delete();
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

    public static boolean isConnectionUnmetered(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) return false;

        return isDeviceOnline(context) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }

    public static String getFormattedFileSize(long fileSize) {
        if (fileSize <= 0) return "0B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
        int digitGroups = (int) (Math.log10(fileSize)/Math.log10(1000));
        return new DecimalFormat("#,##0.#").format(fileSize/Math.pow(1000, digitGroups)) + " " + units[digitGroups];
    }

    public static String getFormattedSpeed(long fileSize) {
        if (fileSize <= 0) return "0B/s";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
        int digitGroups = (int) (Math.log10(fileSize)/Math.log10(1000));
        return String.format("%s/s", new DecimalFormat("#,##0.#").format(fileSize/Math.pow(1000, digitGroups)) + " " + units[digitGroups]);
    }

    public static void cleanupDownloadsDir() {
        File otaFile = new File(Experience.getFreshDir(), "update.zip");
        if (otaFile.exists())
            otaFile.delete();
    }

    @SuppressLint("SetWorldReadable")
    public static void verifyUpdateAsync(Context context) {
        new Thread(() -> {
            SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
            File file = UpdateUtils.getUpdatePackageFile();
            if (file.exists() && verifyPackage(context, file)) {
                //noinspection ResultOfMethodCallIgnored
                file.setReadable(true, false);
                CurrentSoftwareUpdate.setOtaDownloadVerified(context, true);
            } else {
                CurrentSoftwareUpdate.setOtaDownloadVerified(context, false);
            }
        }).start();
    }

    public static boolean verifyPackage(Context context, File updateFile) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
        String md5 = update.getMd5Hash();

        if (TextUtils.isEmpty(md5) || updateFile == null)
            return false;

        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null)
            return false;

        return calculatedDigest.equalsIgnoreCase(md5);
    }

    private static String calculateMD5(File updateFile) {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Unused
            }
        }
    }

    public static void setLastCheckedDate(Context context) {
        Settings.System.putLong(context.getContentResolver(), "SOFTWARE_UPDATE_LAST_CHECKED_DATE", System.currentTimeMillis());
    }

    public static void setSettingAppBadge(Context context, boolean isUpdateAvailable) {
        Settings.System.putInt(context.getContentResolver(), "badge_for_fota", isUpdateAvailable ? 1 : 0);
    }

    public static void startUpdateService(Context context) {
        try {
            if (!UpdateDownloadService.isAvailable())
                context.startService(new Intent(context, UpdateDownloadService.class));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void tryStopUpdateService(Context context) {
        try {
            if (!UpdateDownloadService.isAvailable())
                context.stopService(new Intent(context, UpdateDownloadService.class));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
