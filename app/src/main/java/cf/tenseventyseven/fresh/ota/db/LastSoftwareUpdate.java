package cf.tenseventyseven.fresh.ota.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cf.tenseventyseven.fresh.R;
import cf.tenseventyseven.fresh.ota.SoftwareUpdate;

public class LastSoftwareUpdate {

    private static final String PREFERENCE_DB_NAME = "tns_last_software_update";

    private static final String OTA_DATE_TIME = "ota_date_time";
    private static final String OTA_VERSION_CODE = "ota_version_code";
    private static final String OTA_VERSION_NAME = "ota_version_name";
    private static final String OTA_SPL = "ota_spl";
    private static final String OTA_RELEASE_TYPE = "ota_release_type";
    private static final String OTA_FILE_SIZE = "ota_file_size";
    private static final String OTA_CHANGELOG = "ota_changelog";
    private static final String OTA_UPDATED_APPS = "ota_updated_apps";
    private static final String OTA_RESPONSE = "ota_response";
    private static final String OTA_SUCCESS_DATE = "ota_success_date";

    private static final String DEFAULT_VALUE = "null";

    private static SharedPreferences getPreferenceDb(Context context) {
        return context.getSharedPreferences(PREFERENCE_DB_NAME, Context.MODE_PRIVATE);
    }

    @SuppressLint("ApplySharedPref")
    public static void setSoftwareUpdate(Context context, SoftwareUpdate update) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putLong(OTA_DATE_TIME, update.getDateTime());
        editor.putLong(OTA_VERSION_CODE, update.getVersionCode());
        editor.putString(OTA_VERSION_NAME, update.getVersionName());
        editor.putString(OTA_SPL, update.getSpl());
        editor.putString(OTA_RELEASE_TYPE, update.getReleaseType());
        editor.putLong(OTA_FILE_SIZE, update.getFileSize());
        editor.putString(OTA_CHANGELOG, update.getChangelog());
        editor.putString(OTA_UPDATED_APPS, update.getUpdatedApps());
        editor.putInt(OTA_RESPONSE, 0); // Always start with '0'. This will be set post-OTA if successful.
        editor.commit();
    }

    public static void setSoftwareUpdateResponse(Context context, boolean success) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putInt(OTA_RESPONSE, success ? 200 : 0);
        editor.putLong(OTA_SUCCESS_DATE, success ? System.currentTimeMillis() : 0);
        editor.apply();
    }

    public static SoftwareUpdate getSoftwareUpdate(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        SoftwareUpdate update = new SoftwareUpdate();

        update.setDateTime(prefs.getLong(OTA_DATE_TIME, 0));
        update.setVersionCode(prefs.getLong(OTA_VERSION_CODE, 0));
        update.setVersionName(prefs.getString(OTA_VERSION_NAME, DEFAULT_VALUE));
        update.setSpl(prefs.getString(OTA_SPL, DEFAULT_VALUE));
        update.setReleaseType(prefs.getString(OTA_RELEASE_TYPE, DEFAULT_VALUE));
        update.setFileSize(prefs.getLong(OTA_FILE_SIZE, 0));
        update.setChangelog(prefs.getString(OTA_CHANGELOG, DEFAULT_VALUE));
        update.setUpdatedApps(prefs.getString(OTA_UPDATED_APPS, DEFAULT_VALUE));
        update.setResponse(prefs.getInt(OTA_RESPONSE, 0));

        return update;
    }

    public static List<String> getUpdatedApps(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        List<String> apps = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(prefs.getString(OTA_UPDATED_APPS, "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                apps.add(jsonArray.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return apps;
    }

    public static long getLastDate(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        return Long.parseLong(prefs.getLong(OTA_SUCCESS_DATE, 0) + "");
    }

    @SuppressLint("SimpleDateFormat")
    public static String getLastDateFormat(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        Date date = new Date(prefs.getLong(OTA_SUCCESS_DATE, 0));
        String formatDate = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd MMMM yyyy");
        String formatTime = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm");

        String dateString = DateFormat.format(formatDate, date).toString();
        String timeString = DateFormat.format(formatTime, date).toString();

        return String.format(context.getString(R.string.fresh_ota_changelog_appbar_detail_success_subtitle), dateString, timeString);
    }
}