package io.tenseventyseven.fresh.utils;

import android.content.Context;
import android.provider.Settings;

public class Preferences {

    public static String getDataConnectionIconPackage(Context context) {
        String setting = Settings.System.getString(context.getContentResolver(), "zest_data_icon");
        if (setting == null || setting.isEmpty())
            setting = "io.tns.fresh.data.4g";

        return setting;
    }

    public static void setDataConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_data_icon", selection);
    }

    public static String getWlanConnectionIconPackage(Context context) {
        String setting = Settings.System.getString(context.getContentResolver(), "zest_wlan_icon");
        if (setting == null || setting.isEmpty())
            setting = "io.tns.fresh.wlan.default";

        return setting;
    }

    public static void setWlanConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_wlan_icon", selection);
    }

    public static String getVolteConnectionIconPackage(Context context) {
        String setting = Settings.System.getString(context.getContentResolver(), "zest_volte_icon");
        if (setting == null || setting.isEmpty())
            setting = "io.tns.fresh.volte.default";

        return setting;
    }

    public static String getCurrentAspectRatio(Context context) {
        String setting = Settings.System.getString(context.getContentResolver(), Experience.isDesktopMode(context) ? "zest_display_aspect_ratio_desktop" : "zest_display_aspect_ratio_mobile");
        return (setting == null || setting.isEmpty()) ? "reset" : setting;
    }

    public static void setCurrentAspectRatio(Context context, String ratio) {
        Settings.System.putString(context.getContentResolver(), Experience.isDesktopMode(context) ? "zest_display_aspect_ratio_desktop" : "zest_display_aspect_ratio_mobile", ratio);
    }

    public static void setVolteConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_volte_icon", selection);
    }
}