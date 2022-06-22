package io.tenseventyseven.fresh.ota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;

import org.json.JSONException;
import org.lineageos.updater.download.DownloadClient;

import java.io.File;
import java.io.IOException;

import io.tenseventyseven.fresh.ota.api.UpdateManifest;

public class UpdateCheckReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        UpdateManifest.checkForUpdates(context);
    }

}
