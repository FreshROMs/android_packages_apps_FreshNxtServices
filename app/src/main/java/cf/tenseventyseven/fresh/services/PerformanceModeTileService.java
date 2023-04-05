package cf.tenseventyseven.fresh.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.RemoteViews;

import cf.tenseventyseven.fresh.utils.Performance;
import cf.tenseventyseven.fresh.R;
import cf.tenseventyseven.fresh.zest.sub.PerformanceModeActivity;

public class PerformanceModeTileService extends TileService {
    int mCurrentMode = Performance.PerformanceProfile.BALANCED;
    private BroadcastReceiver mBroadcastReceiver = null;
    String QS_TILE_PERF_MODE_CHANGE_INTENT = "cf.tenseventyseven.fresh.qs.performance_mode_changed";

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        mBroadcastReceiver = new PerfModeChangeReceiver();
    }


    @Override
    public void onStartListening() {
        super.onStartListening();

        if (getQsTile() != null) {
            setTileState(this, Performance.getPerformanceMode());
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
        int newMode = (mCurrentMode + 1) % Performance.PerformanceProfile.TOTAL_PROFILES;
        Performance.setPerformanceMode(newMode);
        setTileState(this, newMode);
    }

    public RemoteViews semGetDetailView() {
        RemoteViews view = new RemoteViews(getPackageName(), R.layout.zest_qs_detail_performance_mode);
        setPerformanceModeOnClick(view);
        refreshRadioButtons(view, Performance.getPerformanceMode());

        return view;
    }

    public Intent semGetSettingsIntent() {
        return new Intent(this, PerformanceModeActivity.class);
    }

    private void setPerformanceModeOnClick(RemoteViews view) {
        Intent gamingIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        Intent defaultIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        Intent multitaskingIntent = new Intent(QS_TILE_PERF_MODE_CHANGE_INTENT);
        gamingIntent.putExtra("newPerfMode", Performance.PerformanceProfile.GAMING);
        defaultIntent.putExtra("newPerfMode", Performance.PerformanceProfile.BALANCED);
        multitaskingIntent.putExtra("newPerfMode", Performance.PerformanceProfile.MULTITASKING);

        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_gaming, PendingIntent.getBroadcast(this, 0, gamingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_default, PendingIntent.getBroadcast(this, 1, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        view.setOnClickPendingIntent(R.id.zest_qs_detail_performance_multitasking, PendingIntent.getBroadcast(this, 2, multitaskingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void refreshRadioButtons(RemoteViews view, int mode)  {
        switch (mode) {
            case Performance.PerformanceProfile.GAMING:
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_gaming);
                break;
            case Performance.PerformanceProfile.MULTITASKING:
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_multitasking);
                break;
            default:
                view.setRadioGroupChecked(R.id.zest_qs_detail_performance_group, R.id.zest_qs_detail_performance_default);
                break;
        }

        view.setTextColor(R.id.zest_qs_detail_performance_gaming, getColor(R.color.sesl4_primary_text_color));
        view.setTextColor(R.id.zest_qs_detail_performance_multitasking, getColor(R.color.sesl4_primary_text_color));
        view.setTextColor(R.id.zest_qs_detail_performance_default, getColor(R.color.sesl4_primary_text_color));
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

    private void setTileState(Context context, int mode) {
        Tile tile = getQsTile();
        String modeName;
        int modeIcon;
        int tileState;

        switch (mode) {
            case Performance.PerformanceProfile.GAMING:
                modeName = context.getString(R.string.zest_performance_setting_option_gaming);
                modeIcon = R.drawable.ic_qs_tile_gaming;
                tileState = Tile.STATE_ACTIVE;
                break;
            case Performance.PerformanceProfile.MULTITASKING:
                modeName = context.getString(R.string.zest_performance_setting_option_multitasking);
                modeIcon = R.drawable.ic_qs_tile_multitasking;
                tileState = Tile.STATE_ACTIVE;
                break;
            default:
                modeName = context.getString(R.string.zest_performance_setting_option_default);
                modeIcon = R.drawable.ic_qs_tile_performance_default;
                tileState = Tile.STATE_INACTIVE;
                break;
        }

        mCurrentMode = mode;

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
            if (context != null && intent != null) {
                String action = intent.getAction();
                if (action.equals(QS_TILE_PERF_MODE_CHANGE_INTENT)) {
                    int newMode = intent.getIntExtra("newPerfMode", Performance.PerformanceProfile.BALANCED);
                    Performance.setPerformanceMode(newMode);
                    setTileState(context, newMode);
                }
            }
        }
    }
}