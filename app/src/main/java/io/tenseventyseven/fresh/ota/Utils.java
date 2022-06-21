package io.tenseventyseven.fresh.ota;

import android.content.Context;
import android.os.SystemProperties;

import java.io.File;

import io.tenseventyseven.fresh.ExperienceUtils;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;

public class Utils {
    public static String PROP_FRESH_OTA_API = SystemProperties.get("ro.fresh.ota.api");
    public static String PROP_FRESH_DEVICE_PRODUCT = SystemProperties.get("ro.fresh.device.product");
    public static String PROP_FRESH_OTA_API_MIRROR = SystemProperties.get("ro.fresh.ota.api.mirror");
    public static String PROP_FRESH_ROM_BRANCH = SystemProperties.get("ro.fresh.build.branch");
    public static String PROP_FRESH_ROM_VERSION_NAME = SystemProperties.get("ro.fresh.version");
    public static String PROP_FRESH_ROM_VERSION_CODE = SystemProperties.get("ro.fresh.build.version");
    public static String PROP_FRESH_ROM_VERSION_UTC = SystemProperties.get("ro.fresh.build.date.utc");

    public static boolean getUpdateAvailability(Context context) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
        String current = PROP_FRESH_ROM_VERSION_CODE;
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
        return new File(ExperienceUtils.getFreshDir(), "update.zip");
    }
}
