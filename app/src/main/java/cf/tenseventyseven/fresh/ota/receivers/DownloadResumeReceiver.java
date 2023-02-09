package cf.tenseventyseven.fresh.ota.receivers;

import static cf.tenseventyseven.fresh.ota.api.UpdateDownloadService.FETCH_GROUP_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import cf.tenseventyseven.fresh.ota.api.UpdateDownload;

public class DownloadResumeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if ((extras != null)) {
            final int requestId = extras.getInt(FETCH_GROUP_ID, -1);
            UpdateDownload.getFetchInstance(context).resume(requestId);
        }
    }
}

