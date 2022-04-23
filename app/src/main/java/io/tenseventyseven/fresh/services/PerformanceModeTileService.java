package io.tenseventyseven.fresh.services;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.Objects;

import io.tenseventyseven.fresh.PerformanceUtils;
import io.tenseventyseven.fresh.R;

public class PerformanceModeTileService extends TileService {
    int mPerformanceModeInt = 1;

    @Override
    public void onStartListening() {
        if (getQsTile() != null) {
            setTileState(this, PerformanceUtils.getPerformanceMode(this));
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        String[] mPerformanceModes = getResources().getStringArray(R.array.zest_performance_setting_values);
        int newMode;
        switch (mPerformanceModeInt) {
            case 1:
                newMode = 2;
                break;
            case 2:
                newMode = 0;
                break;
            default:
                newMode = 1;
                break;
        }

        PerformanceUtils.setPerformanceMode(this, mPerformanceModes[newMode]);
        setTileState(this, mPerformanceModes[newMode]);
    }

    private void setTileState(Context context, String mode) {
        Tile tile = getQsTile();
        int tileState;
        String modeName;
        int modeIcon;

        switch (Objects.requireNonNull(mode)) {
            case "Aggressive":
                modeName = context.getString(R.string.zest_performance_setting_option_gaming);
                modeIcon = R.drawable.ic_qs_tile_gaming;
                tileState = Tile.STATE_ACTIVE;
                mPerformanceModeInt = 0;
                break;
            case "Conservative":
                modeName = context.getString(R.string.zest_performance_setting_option_multitasking);
                modeIcon = R.drawable.ic_qs_tile_multitasking;
                tileState = Tile.STATE_ACTIVE;
                mPerformanceModeInt = 2;
                break;
            default:
                modeName = context.getString(R.string.zest_performance_setting_option_default);
                modeIcon = R.drawable.ic_qs_tile_performance;
                tileState = Tile.STATE_INACTIVE;
                mPerformanceModeInt = 1;
                break;
        }

        tile.setLabel(modeName);
        tile.setIcon(Icon.createWithResource(this, modeIcon));
        tile.setState(tileState);
        tile.updateTile();
    }
}