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

package cf.tenseventyseven.fresh.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;

import java.io.File;

import cf.tenseventyseven.fresh.R;
import cf.tenseventyseven.fresh.ota.SoftwareUpdate;
import cf.tenseventyseven.fresh.ota.UpdateNotifications;
import cf.tenseventyseven.fresh.ota.UpdateUtils;
import cf.tenseventyseven.fresh.ota.api.UpdateCheckJobService;
import cf.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import cf.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;
import cf.tenseventyseven.fresh.utils.Experience;
import cf.tenseventyseven.fresh.utils.Performance;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "FRSH/BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(() -> {
            long bootTime = SystemProperties.getLong("persist.sys.fresh.boot_time", 1);
            long storedTime = Settings.System.getLong(context.getContentResolver(), "fresh_device_boot_time", 0);

            // Don't run the boot time service twice
            if (bootTime == storedTime) {
                Log.i(TAG, "Skipping boot service, we already ran it this session");
                return;
            }

            Log.i(TAG, "Updating device config at boot");
            updateDefaultConfigs(context);
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
}
