package io.tenseventyseven.fresh.ota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.tenseventyseven.fresh.ota.activity.UpdateCheckActivity;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, UpdateCheckActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
