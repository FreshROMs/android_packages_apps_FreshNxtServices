package io.tenseventyseven.fresh.services;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

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

    private void setTileState(boolean state) {
        Tile tile = getQsTile();
        mExtraDimState = state;
        tile.setState(state ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}