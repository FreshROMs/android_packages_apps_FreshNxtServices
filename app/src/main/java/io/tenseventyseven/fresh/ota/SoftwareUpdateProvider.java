package io.tenseventyseven.fresh.ota;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.SystemProperties;

import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;

public class SoftwareUpdateProvider extends ContentProvider {
    private Context mContext;

    public boolean onCreate() {
        mContext = getContext();
        return true;
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        UriMatcher uriMatcher = new UriMatcher(-1);
        uriMatcher.addURI("io.tenseventyseven.fresh.ota.SoftwareUpdateProvider", "otahistory", 1);
        if (uriMatcher.match(uri) == 1) {
            SoftwareUpdate update = LastSoftwareUpdate.getSoftwareUpdate(mContext);
            MatrixCursor matrixCursor = new MatrixCursor(strArr);
            matrixCursor.addRow(new Object[]{LastSoftwareUpdate.getLastDate(mContext), update.getFullVersion(), update.getResponse()});
            return matrixCursor;
        } else {
            throw new IllegalArgumentException("query not implemented : " + uri.getPath());
        }
    }

    private String getVersion() {
        String versionCode = SystemProperties.get("ro.fresh.build.version");
        String dateTime = SystemProperties.get("ro.fresh.build.date.utc");
        String releaseType = SystemProperties.get("ro.fresh.build.branch");
        return versionCode + "/" + dateTime + "/" + releaseType;
    }
}
