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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;

public class UpdateManifest {
    public static String MANIFEST_FILE_NAME = "manifest.json";

    public static String whichServiceReachable(Context context) {
        String manifest_main_url = String.format("%s/%s/%s/",
                SystemProperties.get(UpdateUtils.PROP_FRESH_OTA_API),
                SystemProperties.get(UpdateUtils.PROP_FRESH_DEVICE_PRODUCT),
                SystemProperties.get(UpdateUtils.PROP_FRESH_ROM_VERSION_CODE));

        String manifest_mirror_url = String.format("%s/%s/%s/",
                SystemProperties.get(UpdateUtils.PROP_FRESH_OTA_API_MIRROR),
                SystemProperties.get(UpdateUtils.PROP_FRESH_DEVICE_PRODUCT),
                SystemProperties.get(UpdateUtils.PROP_FRESH_ROM_VERSION_CODE));

        // Just bail out immediately if we are not connected
        if (!UpdateUtils.isDeviceOnline(context))
            return null;

        URL url;
        URLConnection connection;

        // Try connecting to both main API and mirror API
        try {
            url = new URL(manifest_main_url);
            connection = url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.connect();
            return manifest_main_url;
        } catch (IOException e) {
            try {
                url = new URL(manifest_mirror_url);
                connection = url.openConnection();
                connection.setConnectTimeout(10 * 1000);
                connection.connect();
                return manifest_mirror_url;
            } catch (IOException unused) {
                return null;
            }
        }
    }
    public static SoftwareUpdate getUpdateFromManifest(Context context, File json) throws IOException, JSONException {
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
            return null;

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
        return update;
    }

    public static boolean parseManifest(Context context, File json) throws IOException, JSONException {
        SoftwareUpdate update = getUpdateFromManifest(context, json);

        if (update == null)
            return false;

        CurrentSoftwareUpdate.setSoftwareUpdate(context, update);
        return true;
    }

    public static boolean getUpdateAvailability(Context context) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(context);
        String current = SystemProperties.get(UpdateUtils.PROP_FRESH_ROM_VERSION_CODE);
        String manifest = Long.toString(update.getVersionCode());

        if (current.length() > manifest.length()) {
            StringBuilder manifestBuilder = new StringBuilder(manifest);
            for (int i = 0; i < current.length() - manifestBuilder.length(); i++) {
                manifestBuilder.append("0");
            }
            manifest = manifestBuilder.toString();
        } else if (manifest.length() > current.length()) {
            StringBuilder currentBuilder = new StringBuilder(current);
            for (int i = 0; i < manifest.length() - currentBuilder.length(); i++) {
                currentBuilder.append("0");
            }
            current = currentBuilder.toString();
        }

        return Long.parseLong(current) < Long.parseLong(manifest);
    }
}
