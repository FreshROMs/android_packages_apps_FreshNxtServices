/*
 * Copyright (C) 2020 The Proton AOSP Project
 * Extensions 2021 TenSeventy7 for The Fresh Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tensevntysevn.fresh.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.SystemPropertiesProto;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import io.tensevntysevn.fresh.R;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "FRSH/DeviceConfig";

    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(() -> {
            // Set virtual memory (RAM Plus) size on Device Care
            if (getProp("ro.fresh.device.hw.ramplus").equalsIgnoreCase("true")) {
                Log.i(TAG, "Updating virtual RAM configuration");
                setVirtMemSize(context, getProp("ro.fresh.device.rp.size"));
            }

            Log.i(TAG, "Updating device config at boot");
            updateDefaultConfigs(context);
        }).start();
    }

    private void updateDefaultConfigs(Context context) {
        updateConfig(context, R.array.configs_base);
        updateConfig(context, R.array.configs_device);
    }

    private void updateConfig(Context context, int configArray) {
        // Set current properties
        String[] rawProperties = context.getResources().getStringArray(configArray);
        for (String property : rawProperties) {
            // Format: namespace/key=value
            String[] kv = property.split("=");
            String fullKey = kv[0];
            String[] nsKey = fullKey.split("/");

            String namespace = nsKey[0];
            String key = nsKey[1];
            String value = "";
            if (kv.length > 1) {
                value = kv[1];
            }

            DeviceConfig.setProperty(namespace, key, value, true);
        }
    }

    private void setVirtMemSize(Context context, String size) {
        int vramSize = (int) Integer.parseInt(size);
        Settings.Global.putInt(context.getContentResolver(), "BKDEV_UX_KEY", vramSize);
    }

    public static String getProp(String propName) {
        Process p;
        String result = "";
        try {
            p = new ProcessBuilder("/system/bin/getprop", propName).redirectErrorStream(true)
                    .start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                result = line;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
