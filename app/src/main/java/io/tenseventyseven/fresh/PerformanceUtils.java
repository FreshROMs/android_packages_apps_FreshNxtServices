package io.tenseventyseven.fresh;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class PerformanceUtils {
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
            return null;
    }

    public static void setPerformanceMode(Context context, String str) {
        try {
            ContentValues values = new ContentValues();
            values.put("MODE", str);
            context.getContentResolver().update(perfContentProvider, values, null, null);
        } catch (Exception unused) {
            // Unused
        }
    }
}