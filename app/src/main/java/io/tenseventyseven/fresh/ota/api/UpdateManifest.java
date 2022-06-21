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
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.IntRange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import io.tenseventyseven.fresh.ota.Utils;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;

public class UpdateManifest {
    public static int MANIFEST_SUCCESS = 0;
    public static int MANIFEST_FAILED_NO_CONNECTION = 1;
    public static int MANIFEST_FAILED_INVALID = 2;
    public static int MANIFEST_FAILED_UNSUPPORTED = 3;
    public static int MANIFEST_FAILED_FILE_NOT_FOUND = 4;
    public static int MANIFEST_FAILED_UNKNOWN = -1;

    public static int downloadManifest(Context context) {
        if (getConnectionType(context) <= 0)
            return MANIFEST_FAILED_NO_CONNECTION;

        try {
            InputStream input;

            String urlFormat = String.format("%s/%s/%s/", Utils.PROP_FRESH_OTA_API, Utils.PROP_FRESH_DEVICE_PRODUCT, Utils.PROP_FRESH_ROM_VERSION_CODE);
            URL url = new URL(urlFormat);
            URLConnection connection = url.openConnection();
            connection.connect();

            input = new BufferedInputStream(url.openStream());
            File manifestFile = new File(context.getFilesDir(), "manifest.json");

            if (manifestFile.exists())
                manifestFile.delete();

            try (OutputStream output = new FileOutputStream(manifestFile)) {
                byte[] buffer = new byte[4096];
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                output.flush();
            } catch (IOException e) {
                manifestFile.delete();
                e.printStackTrace();
            }

            input.close();
            return MANIFEST_SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return MANIFEST_FAILED_UNKNOWN;
        }
    }

    public static int parseManifest(Context context) {
        SoftwareUpdate update = new SoftwareUpdate();
        File manifestFile = new File(context.getFilesDir(), "manifest.json");
        StringBuilder sb;

        try {
            FileInputStream is = new FileInputStream(manifestFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return MANIFEST_FAILED_FILE_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
            return MANIFEST_FAILED_INVALID;
        }

        try {
            if (sb.toString().isEmpty())
                return MANIFEST_FAILED_INVALID;

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

            return MANIFEST_SUCCESS;
        } catch (JSONException e) {
            e.printStackTrace();
            return MANIFEST_FAILED_INVALID;
        }

    }

    @IntRange(from = 0, to = 3)
    public static int getConnectionType(Context context) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = 2;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    result = 1;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    result = 3;
                }
            }
        }
        return result;
    }
}
