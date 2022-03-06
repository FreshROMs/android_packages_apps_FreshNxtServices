package io.tenseventyseven.fresh.utils;

import android.content.Context;
import android.provider.Settings;

public class Preferences {

    public static String getDataConnectionIconPackage(Context context) {
        return Settings.System.getString(context.getContentResolver(), "zest_data_icon");
    }

    public static void setDataConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_data_icon", selection);
    }

    public static String getWlanConnectionIconPackage(Context context) {
        return Settings.System.getString(context.getContentResolver(), "zest_wlan_icon");
    }

    public static void setWlanConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_wlan_icon", selection);
    }
}