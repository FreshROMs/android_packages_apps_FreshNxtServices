package io.tenseventyseven.fresh.services;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.RemoteViews;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.zest.sub.VideoBrightnessActivity;

public class VideoBrightnessTileService extends TileService {

    boolean mVideoBrightnessState = false;

    @Override
    public void onStartListening() {
        if (getQsTile() != null) {
            setTileState(VideoBrightnessActivity.getVideoBrightnessState(this));
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        VideoBrightnessActivity.setVideoBrightnessState(this, !mVideoBrightnessState);
        setTileState(!mVideoBrightnessState);
    }

    public RemoteViews semGetDetailView() {
        RemoteViews view = new RemoteViews(getPackageName(), R.layout.zest_qs_detail_video_brightness);
        return view;
    }

    public CharSequence semGetDetailViewTitle() {
        return getString(R.string.zest_hdr_effect_setting_title);
    }

    public Intent semGetSettingsIntent() {
        Intent intent = new Intent(this, VideoBrightnessActivity.class);
        return intent;
    }

    public boolean semIsToggleButtonChecked() {
        return VideoBrightnessActivity.getVideoBrightnessState(this);
    }

    public void semSetToggleButtonChecked(boolean z) {
        VideoBrightnessActivity.setVideoBrightnessState(this, z);
        setTileState(z);
    }

    public boolean semIsToggleButtonExists() {
        return true;
    }

    private void setTileState(boolean state) {
        Tile tile = getQsTile();
        mVideoBrightnessState = state;
        tile.setState(state ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}