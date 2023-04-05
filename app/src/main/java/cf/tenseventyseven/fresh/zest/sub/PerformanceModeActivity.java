package cf.tenseventyseven.fresh.zest.sub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import cf.tenseventyseven.fresh.utils.Performance;
import cf.tenseventyseven.fresh.R;

public class PerformanceModeActivity extends AppCompatActivity {

    @BindView(R.id.zest_performance_mode_toolbar)
    ToolbarLayout toolbar;

    @BindView(R.id.zest_performance_mode_gaming)
    LinearLayout mPerformanceGaming;
    @BindView(R.id.zest_performance_mode_default)
    LinearLayout mPerformanceDefault;
    @BindView(R.id.zest_performance_mode_multitasking)
    LinearLayout mPerformanceMultitasking;

    @BindView(R.id.radio_performance_mode_gaming)
    RadioButton mPerformanceRadioGaming;
    @BindView(R.id.radio_performance_mode_default)
    RadioButton mPerformanceRadioDefault;
    @BindView(R.id.radio_performance_mode_multitasking)
    RadioButton mPerformanceRadioMultitasking;

    private PerformanceModeSettingsObserver mSettingsObserver;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_performance_mode_settings);
        ButterKnife.bind(this);

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new PerformanceModeSettingsObserver(mHandler);

        refreshRadioButtons(Performance.getPerformanceMode());

        mPerformanceGaming.setOnClickListener(onTapOption(this, Performance.PerformanceProfile.GAMING));
        mPerformanceDefault.setOnClickListener(onTapOption(this, Performance.PerformanceProfile.BALANCED));
        mPerformanceMultitasking.setOnClickListener(onTapOption(this, Performance.PerformanceProfile.MULTITASKING));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSettingsObserver != null) {
            mSettingsObserver.setListening(true);
        }
    }

    @Override
    protected void onPause() {
        if (mSettingsObserver != null) {
            mSettingsObserver.setListening(false);
        }

        super.onPause();
    }

    private void refreshRadioButtons(int mode) {
        switch (mode) {
            case Performance.PerformanceProfile.GAMING:
                mPerformanceRadioGaming.setChecked(true);
                mPerformanceRadioDefault.setChecked(false);
                mPerformanceRadioMultitasking.setChecked(false);
                break;
            case Performance.PerformanceProfile.MULTITASKING:
                mPerformanceRadioGaming.setChecked(false);
                mPerformanceRadioDefault.setChecked(false);
                mPerformanceRadioMultitasking.setChecked(true);
                break;
           default:
                mPerformanceRadioGaming.setChecked(false);
                mPerformanceRadioDefault.setChecked(true);
                mPerformanceRadioMultitasking.setChecked(false);
                break;
        }
    }

    private View.OnClickListener onTapOption(Context context, int mode) {
        return v -> {
            refreshRadioButtons(mode);
            Performance.setPerformanceMode(context, mode);
        };
    }

    private final class PerformanceModeSettingsObserver extends ContentObserver {
        private final Uri SETTING_URI = Settings.System.getUriFor("zest_system_performance_mode");

        public PerformanceModeSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            String mode = Settings.System.getString(getContentResolver(), "zest_system_performance_mode");
            if (mode == null || mode.isEmpty())
                mode = "Default";
            refreshRadioButtons(Integer.parseInt(mode));
        }

        public void setListening(boolean z) {
            if (z) {
                getContentResolver().registerContentObserver(this.SETTING_URI, false, this);
            } else {
                getContentResolver().unregisterContentObserver(this);
            }
        }
    }
}