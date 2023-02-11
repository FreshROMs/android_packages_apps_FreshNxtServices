package cf.tenseventyseven.fresh.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DeviceConfig;
import android.provider.Settings;

import java.util.Objects;

import cf.tenseventyseven.fresh.R;

public class Performance {
    private static final String FRESH_PERFORMANCE_MODE = "fresh_performance_mode";
    public static class PerformanceProfile {
        public static final int BALANCED = 0;
        public static final int MULTITASKING = 1;
        public static final int GAMING = 2;
        public static final int TOTAL_PROFILES = 3;

    }

    public static int getPerformanceMode() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_CONFIGURATION, FRESH_PERFORMANCE_MODE, PerformanceProfile.BALANCED);
    }

    public static String getPerformanceModeString(Context context) {
        switch (getPerformanceMode()) {
            case PerformanceProfile.GAMING:
                return context.getString(R.string.zest_performance_setting_option_gaming);
            case PerformanceProfile.MULTITASKING:
                return context.getString(R.string.zest_performance_setting_option_multitasking);
            default:
                return context.getString(R.string.zest_performance_setting_option_default);
        }
    }

    public static void setPerformanceMode(Context context, int newMode) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CONFIGURATION, FRESH_PERFORMANCE_MODE, Integer.toString(newMode), true);
        Settings.System.putString(context.getContentResolver(), "zest_system_performance_mode", Integer.toString(newMode));
    }
}