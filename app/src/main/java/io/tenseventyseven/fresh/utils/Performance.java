package io.tenseventyseven.fresh.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import java.util.Objects;

import io.tenseventyseven.fresh.R;

public class Performance {
    public static final Uri perfContentProvider = Uri.parse("content://com.android.server.chimera.provider/v1");
    public static String mPerformanceMode;

    private static boolean queryPerformanceModes(Context context) {
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(perfContentProvider, null, null, null, null);
        } catch (Exception unused) {
            return false;
        }

        if (cursor == null || !cursor.moveToNext()) {
            if (cursor == null || cursor.isClosed()) {
                return false;
            }
            cursor.close();
            return false;
        }

        mPerformanceMode = cursor.getString(cursor.getColumnIndexOrThrow("CURRENT_MODE"));
        cursor.close();
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return true;
    }

    public static String getPerformanceMode(Context context) {
        if (queryPerformanceModes(context))
            return mPerformanceMode;
        else
            return "Default";
    }

    public static String getPerformanceModeString(Context context) {
        String currentMode = getPerformanceMode(context);
        switch (Objects.requireNonNull(currentMode)) {
            case "Aggressive":
                return context.getString(R.string.zest_performance_setting_option_gaming);
            case "Conservative":
                return context.getString(R.string.zest_performance_setting_option_multitasking);
            default:
                return context.getString(R.string.zest_performance_setting_option_default);
        }
    }

    public static void setPerformanceMode(Context context, String str) {
        try {
            ContentValues values = new ContentValues();
            values.put("MODE", str);
            context.getContentResolver().update(perfContentProvider, values, null, null);
            Settings.System.putString(context.getContentResolver(), "zest_system_performance_mode", str);
        } catch (Exception unused) {
            // Unused
        }
    }
}