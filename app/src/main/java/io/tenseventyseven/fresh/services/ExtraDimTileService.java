package io.tenseventyseven.fresh.services;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.RemoteViews;

import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.zest.sub.ExtraDimSettingsActivity;

public class ExtraDimTileService extends TileService {

    boolean mExtraDimState = false;

    @Override
    public void onStartListening() {
        if (getQsTile() != null) {
            setTileState(ExtraDimSettingsActivity.getExtraDimState(this));
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        ExtraDimSettingsActivity.setExtraDimState(this, !mExtraDimState);
        setTileState(!mExtraDimState);
    }

    public RemoteViews semGetDetailView() {
        RemoteViews view = new RemoteViews(getPackageName(), R.layout.zest_qs_detail_extra_dim);
        return view;
    }

    public CharSequence semGetDetailViewTitle() {
        return getString(R.string.zest_extra_dim_setting_title);
    }

    public Intent semGetSettingsIntent() {
        Intent intent = new Intent(this, ExtraDimSettingsActivity.class);
        return intent;
    }

    public boolean semIsToggleButtonChecked() {
        return ExtraDimSettingsActivity.getExtraDimState(this);
    }

    public void semSetToggleButtonChecked(boolean z) {
        ExtraDimSettingsActivity.setExtraDimState(this, z);
        setTileState(z);
    }

    public boolean semIsToggleButtonExists() {
        return true;
    }

    private void setTileState(boolean state) {
        Tile tile = getQsTile();
        mExtraDimState = state;
        tile.setState(state ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}