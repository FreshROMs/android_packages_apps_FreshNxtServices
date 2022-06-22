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

package io.tenseventyseven.fresh.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;

import java.io.File;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.Utils;
import io.tenseventyseven.fresh.ota.api.UpdateManifest;
import io.tenseventyseven.fresh.utils.Experience;
import io.tenseventyseven.fresh.zest.sub.ExtraDimSettingsActivity;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "FRSH/BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(() -> {
            Log.i(TAG, "Checking device provisioning");
            checkInstallProvisioning(context);
            Log.i(TAG, "Updating device config at boot");
            updateDefaultConfigs(context);
            Log.i(TAG, "Setting up notification channels for services");
            Utils.setupNotificationChannels(context);

            Log.i(TAG, "Successfully booted. Welcome to FreshROMs!");
            Utils.cleanupDownloadsDir();
            Utils.scheduleRepeatingUpdatesCheck(context);
        }).start();
    }

    private void updateDefaultConfigs(Context context) {
        updateConfig(context, R.array.configs_base, false);
        updateConfig(context, R.array.configs_base_soft, true);
        updateConfig(context, R.array.configs_device, false);
    }

    private void updateConfig(Context context, int configArray, boolean isSoft) {
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

            // Skip soft configs that already have values
            if (!isSoft || DeviceConfig.getString(namespace, key, null) == null) {
                DeviceConfig.setProperty(namespace, key, value, true);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkInstallProvisioning(Context context) {
        File folder = Experience.getFreshDir();
        boolean isProvisioned = Settings.System.getInt(context.getContentResolver(), Experience.FRESH_DEVICE_PROVISION_KEY, 0) == 1;

        // If we are not provisioned, delete the Fresh folder and make it again
        // so we don't transfer over old settings (i.e. fingerprint animation style) to a new installation.
        if (!isProvisioned && folder.exists()) {
            folder.delete();
            folder.mkdir();
            Settings.System.putInt(context.getContentResolver(), Experience.FRESH_DEVICE_PROVISION_KEY, 1);
        }
    }
}
