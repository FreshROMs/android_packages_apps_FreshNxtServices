package io.tenseventyseven.fresh.utils;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.util.DisplayMetrics;

import java.util.Scanner;

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

    public static String getCurrentAspectRatio(Context context, Activity activity) {
        String setting = Settings.System.getString(context.getContentResolver(), "zest_display_aspect_ratio");

        if (setting == null || setting.isEmpty() || setting.equalsIgnoreCase("reset"))
            return "reset";

        Scanner valueScan = new Scanner(setting);
        String scannerDelimit = ":";
        valueScan.useDelimiter(scannerDelimit);
        int heightRatio = Integer.parseInt(valueScan.next());
        int widthRatio = Integer.parseInt(valueScan.next());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int width = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int height = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);

        if ((width / widthRatio) != (height / heightRatio)) {
            setCurrentAspectRatio(context, "reset");
            return "reset";
        }

        return setting;
    }

    public static void setCurrentAspectRatio(Context context, String ratio) {
        Settings.System.putString(context.getContentResolver(), "zest_display_aspect_ratio", ratio);
    }

    public static void setVolteConnectionIconPackage(Context context, String selection) {
        Settings.System.putString(context.getContentResolver(), "zest_volte_icon", selection);
    }
}