package io.tenseventyseven.fresh.ota.api;
/*
 * Copyright (C) 2015 Matt Booth.
 *
 * Licensed under the Attribution-NonCommercial-ShareAlike 4.0 International
 * (the "License") you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://creativecommons.org/licenses/by-nc-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lineageos.updater.download.DownloadClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.tenseventyseven.fresh.ota.Utils;
import io.tenseventyseven.fresh.ota.activity.DeviceUpdatedActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateAvailableActivity;
import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;

public class UpdateManifest {
    public static final int MANIFEST_SUCCESS = 0;
    public static final int MANIFEST_FAILED_NO_CONNECTION = 1;
    public static final int MANIFEST_SKIP_PENDING_UPDATE = 2;
    public static final int MANIFEST_FAILED_UNKNOWN = -1;

    private static final String MANIFEST_SETTING_USE_MIRROR = "fresh_ota_use_mirror_api";

    public static int checkForUpdates(Context context) {
        if (!Utils.isDeviceOnline(context)) {
            Utils.scheduleUpdatesCheck(context);
            return MANIFEST_FAILED_NO_CONNECTION;
        }

        if (Utils.getUpdateAvailability(context)) {
            Utils.showNewUpdateNotification(context);
            Utils.scheduleUpdatesCheck(context);
            return MANIFEST_SKIP_PENDING_UPDATE;
        }

        // Show checking notification
        Utils.showOngoingCheckNotification(context);

        final File json = new File(context.getFilesDir(), "manifest.json");
        String url = String.format("%s/%s/%s/",
                SystemProperties.get(Utils.PROP_FRESH_OTA_API),
                SystemProperties.get(Utils.PROP_FRESH_DEVICE_PRODUCT),
                SystemProperties.get(Utils.PROP_FRESH_ROM_VERSION_CODE));

        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(boolean cancelled) {
                Utils.scheduleUpdatesCheck(context);
                Utils.cancelOngoingCheckNotification(context);
            }

            @Override
            public void onResponse(DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess() {
                try {
                    if (!json.exists())
                        return;

                    if (!parseManifest(context, json))
                        return;

                    if (Utils.getUpdateAvailability(context)) {
                        Utils.updateRepeatingUpdatesCheck(context);
                    }

                    // In case we set a one-shot check because of a previous failure
                    Utils.cancelUpdatesCheck(context);
                } catch (IOException | JSONException e) {
                    Utils.scheduleUpdatesCheck(context);
                }
            }
        };

        try {
            DownloadClient downloadClient = new DownloadClient.Builder()
                    .setUrl(url)
                    .setDestination(json)
                    .setDownloadCallback(callback)
                    .build();
            downloadClient.start();
        } catch (IOException e) {
            try {
                url = String.format("%s/%s/%s/",
                        SystemProperties.get(Utils.PROP_FRESH_OTA_API_MIRROR),
                        SystemProperties.get(Utils.PROP_FRESH_DEVICE_PRODUCT),
                        SystemProperties.get(Utils.PROP_FRESH_ROM_VERSION_CODE));
                DownloadClient downloadClient = new DownloadClient.Builder()
                        .setUrl(url)
                        .setDestination(json)
                        .setDownloadCallback(callback)
                        .build();
                downloadClient.start();
            } catch (IOException x) {
                x.printStackTrace();
                Utils.scheduleUpdatesCheck(context);
                return MANIFEST_FAILED_UNKNOWN;
            }
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (Utils.getUpdateAvailability(context))
                Utils.showNewUpdateNotification(context);

            Utils.cancelOngoingCheckNotification(context);
            Settings.System.putInt(context.getContentResolver(), "badge_for_fota", Utils.getUpdateAvailability(context) ? 1 : 0);
            Settings.System.putLong(context.getContentResolver(), "SOFTWARE_UPDATE_LAST_CHECKED_DATE", System.currentTimeMillis());
        }, 2000);

        return MANIFEST_SUCCESS;
    }

    public static boolean parseManifest(Context context, File json) throws IOException, JSONException {
        SoftwareUpdate update = new SoftwareUpdate();
        StringBuilder sb;

        FileInputStream is = new FileInputStream(json);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        reader.close();

        if (sb.toString().isEmpty())
            return false;

        JSONObject jObj = new JSONObject(sb.toString());
        JSONObject romObj = jObj.getJSONObject("response");

        update.setDateTime(romObj.getLong("datetime"));
        update.setVersionCode(romObj.getLong("versionCode"));
        update.setVersionName(romObj.getString("versionName"));
        update.setSpl(romObj.getString("spl"));
        update.setMd5Hash(romObj.getString("hash"));
        update.setReleaseType(romObj.getString("romtype"));
        update.setFileUrl(romObj.getString("url"));
        update.setFileSize(romObj.getLong("size"));
        update.setChangelog(romObj.getString("changelog"));

        PackageManager pm = context.getPackageManager();
        JSONArray updatedApps = romObj.getJSONArray("updatedapps");
        JSONArray packageArray = new JSONArray();

        for (int i = 0; i < updatedApps.length(); i++) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(updatedApps.getString(i), 0);
                packageArray.put(pm.getApplicationLabel(info).toString());
            } catch (PackageManager.NameNotFoundException ignored) {
                // App not found
            }
        }

        update.setUpdatedApps(packageArray.toString());
        CurrentSoftwareUpdate.setSoftwareUpdate(context, update);

        return true;
    }
}
