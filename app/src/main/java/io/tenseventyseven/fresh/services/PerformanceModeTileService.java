package io.tenseventyseven.fresh.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.RemoteViews;

import java.util.Objects;

import io.tenseventyseven.fresh.utils.Performance;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.zest.sub.PerformanceModeActivity;

public class PerformanceModeTileService extends TileService {
    int mPerformanceModeInt = 1;
    private BroadcastReceiver mBroadcastReceiver = null;
    int[] mPerformanceModesInt = {1, 2, 0};
    String QS_TILE_PERF_MODE_CHANGE_INTENT = "io.tenseventyseven.fresh.qs.performance_mode_changed";

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        mBroadcastReceiver = new PerfModeChangeReceiver();
    }


    @Override
    public void onStartListening() {
        super.onStartListening();

        if (getQsTile() != null) {
            setTileState(this, Performance.getPerformanceMode(this));
        }

        if (mBroadcastReceiver != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(QS_TILE_PERF_MODE_CHANGE_INTENT);
            registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    @Override
    public void onStopListening() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        super.onStopListening();
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

        Performance.setPerformanceMode(this, mPerformanceModes[newMode]);
        setTileState(this, mPerformanceModes[newMode]);
    }

    public RemoteViews semGetDetailView() {
        RemoteViews view = new RemoteViews(getPackageName(), R.layout.zest_qs_detail_performance_mode);
        setPerformanceModeOnClick(view);
        refreshRadioButtons(view, Performance.getPerformanceMode(this));

        return view;
    }

    public Intent semGetSettingsIntent() {
        Intent intent = new Intent(this, PerformanceModeActivity.class);
        return intent;
    }

    private void setPerformanceModeOnClick(RemoteViews view) {
        Intent gamingIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        Intent defaultIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        Intent multitaskingIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        gamingIntent.putExtra("newPerfMode", 0);
        defaultIntent.putExtra("newPerfMode", 1);
        multitaskingIntent.putExtra("newPerfMode", 2);

        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_gaming, PendingIntent.getBroadcast(this, 0, gamingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_default, PendingIntent.getBroadcast(this, 1, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_multitasking, PendingIntent.getBroadcast(this, 2, multitaskingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void refreshRadioButtons(RemoteViews view, String mode)  {
        switch (mode) {
            case "Aggressive":
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_gaming);
                break;
            case "Conservative":
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_multitasking);
                break;
            default:
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_default);
                break;
        }

        view.setTextColor(R.id.zest_qs_detail_performance_gaming, getColor(R.color.oui_primary_text_color_color));
        view.setTextColor(R.id.zest_qs_detail_performance_multitasking, getColor(R.color.oui_primary_text_color_color));
        view.setTextColor(R.id.zest_qs_detail_performance_default, getColor(R.color.oui_primary_text_color_color));
    }

    public CharSequence semGetDetailViewTitle() {
        return getString(R.string.zest_performance_mode_title);
    }

    public boolean semIsToggleButtonChecked() {
        return false;
    }

    public boolean semIsToggleButtonExists() {
        return false;
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
                modeIcon = R.drawable.ic_qs_tile_performance_default;
                tileState = Tile.STATE_INACTIVE;
                mPerformanceModeInt = 1;
                break;
        }

        tile.setLabel(modeName);
        tile.setIcon(Icon.createWithResource(this, modeIcon));
        tile.setState(tileState);
        tile.updateTile();
    }

    public class PerfModeChangeReceiver extends BroadcastReceiver {
        PerfModeChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (context != null || intent != null) {
                String action = intent.getAction();
                if (action.equals(QS_TILE_PERF_MODE_CHANGE_INTENT)) {
                    String mode;
                    int newMode = intent.getIntExtra("newPerfMode", 1);
                    switch (newMode) {
                        case 0:
                            mode = "Aggressive";
                            break;
                        case 2:
                            mode = "Conservative";
                            break;
                        default:
                            mode = "Default";
                            break;
                    }
                    Performance.setPerformanceMode(context, mode);
                    setTileState(context, mode);
                }
            }
        }
    }
}