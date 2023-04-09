package cf.tenseventyseven.fresh.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DeviceConfig;
import android.provider.Settings;

import cf.tenseventyseven.fresh.R;

public class Performance {
    private static final String FRESH_PERFORMANCE_MODE = "fresh_performance_mode";
    public static final Uri MEMORY_MODE_PROVIDER = Uri.parse("content://com.android.server.chimera.provider/v1");
    public static class PerformanceProfile {
        public static final int BALANCED = 0;
        public static final int MULTITASKING = 1;
        public static final int GAMING = 2;
        public static final int TOTAL_PROFILES = 3;

    }
    private static class MemoryMode {
        public static final String DEFAULT = "Default";
        public static final String CONSERVATIVE = "Conservative";

        public static String getModeFromPerf(int performanceMode) {
            if (performanceMode == PerformanceProfile.MULTITASKING) {
                return CONSERVATIVE;
            }

            return DEFAULT;
        }
    }

    private static void setMemoryMode(Context context, int mode) {
        String newMode = MemoryMode.getModeFromPerf(mode);

        try {
            ContentValues values = new ContentValues();
            values.put("MODE", newMode);
            context.getContentResolver().update(MEMORY_MODE_PROVIDER, values, null, null);
        } catch (Exception unused) {
            // Unused
        }
    }

    public static String getPerformanceModeString(Context context) {
        switch (getPerformanceMode(context)) {
            case PerformanceProfile.GAMING:
                return context.getString(R.string.zest_performance_setting_option_gaming);
            case PerformanceProfile.MULTITASKING:
                return context.getString(R.string.zest_performance_setting_option_multitasking);
            default:
                return context.getString(R.string.zest_performance_setting_option_default);
        }
    }

    public static int getPerformanceMode(Context context) {
        int currentMode = DeviceConfig.getInt(DeviceConfig.NAMESPACE_CONFIGURATION, FRESH_PERFORMANCE_MODE, PerformanceProfile.BALANCED);
        Cursor cursor;
        String memoryMode;

        try {
            cursor = context.getContentResolver().query(MEMORY_MODE_PROVIDER, null, null, null, null);
        } catch (Exception unused) {
            return currentMode;
        }

        if (cursor == null || !cursor.moveToNext()) {
            if (cursor == null || cursor.isClosed()) {
                return currentMode;
            }
            cursor.close();
            return currentMode;
        }

        memoryMode = cursor.getString(cursor.getColumnIndexOrThrow("CURRENT_MODE"));

        cursor.close();
        if (!cursor.isClosed()) {
            cursor.close();
        }

        // If system and FreshServices disagree, prefer Zest's
        if (!memoryMode.equalsIgnoreCase(MemoryMode.getModeFromPerf(currentMode)))
            setMemoryMode(context, currentMode);

        return currentMode;
    }

    public static void setPerformanceMode(Context context, int newMode) {
        setMemoryMode(context, newMode);
        Settings.System.putString(context.getContentResolver(), "fresh_system_performance_mode", Integer.toString(newMode));
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CONFIGURATION, FRESH_PERFORMANCE_MODE, Integer.toString(newMode), true);
    }
}