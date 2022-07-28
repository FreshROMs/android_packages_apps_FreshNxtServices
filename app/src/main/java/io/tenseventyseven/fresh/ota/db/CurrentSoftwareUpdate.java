package io.tenseventyseven.fresh.ota.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import io.tenseventyseven.fresh.ota.SoftwareUpdate;

public class CurrentSoftwareUpdate {

    private static final String PREFERENCE_DB_NAME = "tns_current_software_update";

    private static final String OTA_DATE_TIME = "ota_date_time";
    private static final String OTA_VERSION_CODE = "ota_version_code";
    private static final String OTA_VERSION_NAME = "ota_version_name";
    private static final String OTA_SPL = "ota_spl";
    private static final String OTA_RELEASE_TYPE = "ota_release_type";
    private static final String OTA_FILE_HASH = "ota_file_hash";
    private static final String OTA_FILE_URL = "ota_file_url";
    private static final String OTA_FILE_SIZE = "ota_file_size";
    private static final String OTA_CHANGELOG = "ota_changelog";
    private static final String OTA_UPDATED_APPS = "ota_updated_apps";

    private static final String OTA_DOWNLOAD_ID = "ota_download_id";
    private static final String OTA_DOWNLOAD_ETA = "ota_download_eta";
    private static final String OTA_DOWNLOAD_PROGRESS = "ota_download_progress";

    private static final String OTA_INSTALL_STATE = "ota_install_state";

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
        editor.putString(OTA_FILE_URL, update.getFileUrl());
        editor.putLong(OTA_FILE_SIZE, update.getFileSize());
        editor.putString(OTA_FILE_HASH, update.getMd5Hash());
        editor.putString(OTA_CHANGELOG, update.getChangelog());
        editor.putString(OTA_UPDATED_APPS, update.getUpdatedApps());
        editor.putInt(OTA_INSTALL_STATE, SoftwareUpdate.OTA_INSTALL_STATE_NOT_STARTED);
        editor.putInt(OTA_DOWNLOAD_PROGRESS, 0);
        editor.putLong(OTA_DOWNLOAD_ETA, 0);
        editor.commit();
    }

    public static SoftwareUpdate getSoftwareUpdate(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        SoftwareUpdate update = new SoftwareUpdate();

        update.setDateTime(prefs.getLong(OTA_DATE_TIME, 0));
        update.setVersionCode(prefs.getLong(OTA_VERSION_CODE, 0));
        update.setVersionName(prefs.getString(OTA_VERSION_NAME, DEFAULT_VALUE));
        update.setSpl(prefs.getString(OTA_SPL, DEFAULT_VALUE));
        update.setReleaseType(prefs.getString(OTA_RELEASE_TYPE, DEFAULT_VALUE));
        update.setFileUrl(prefs.getString(OTA_FILE_URL, DEFAULT_VALUE));
        update.setFileSize(prefs.getLong(OTA_FILE_SIZE, 0));
        update.setMd5Hash(prefs.getString(OTA_FILE_HASH, DEFAULT_VALUE));
        update.setChangelog(prefs.getString(OTA_CHANGELOG, DEFAULT_VALUE));
        update.setUpdatedApps(prefs.getString(OTA_UPDATED_APPS, DEFAULT_VALUE));

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

    @SuppressLint("ApplySharedPref")
    public static void setOtaDownloadId(Context context, int id) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putInt(OTA_DOWNLOAD_ID, id);
        editor.commit();
    }

    public static int getOtaDownloadId(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        return prefs.getInt(OTA_DOWNLOAD_ID, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void setOtaState(Context context, int state) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putInt(OTA_INSTALL_STATE, state);
        editor.commit();
    }

    public static int getOtaState(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        return prefs.getInt(OTA_INSTALL_STATE, SoftwareUpdate.OTA_INSTALL_STATE_UNKNOWN);
    }

    @SuppressLint("ApplySharedPref")
    public static void setOtaDownloadProgress(Context context, int val) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putInt(OTA_DOWNLOAD_PROGRESS, val);
        editor.commit();
    }

    public static int getOtaDownloadProgress(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        return prefs.getInt(OTA_DOWNLOAD_PROGRESS, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void setOtaDownloadEta(Context context, long val) {
        SharedPreferences.Editor editor = getPreferenceDb(context).edit();
        editor.putLong(OTA_DOWNLOAD_ETA, val);
        editor.commit();
    }

    public static long getOtaDownloadEta(Context context) {
        SharedPreferences prefs = getPreferenceDb(context);
        return prefs.getLong(OTA_DOWNLOAD_ETA, 0);
    }
}