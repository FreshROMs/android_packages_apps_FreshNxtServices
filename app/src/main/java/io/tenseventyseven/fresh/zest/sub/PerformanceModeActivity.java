package io.tenseventyseven.fresh.zest.sub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.Preference;
import io.tenseventyseven.fresh.PerformanceUtils;
import io.tenseventyseven.fresh.R;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_performance_mode_settings);
        ButterKnife.bind(this);

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        refreshRadioButtons(PerformanceUtils.getPerformanceMode(this));

        mPerformanceGaming.setOnClickListener(onTapOption(this, "Aggressive"));
        mPerformanceDefault.setOnClickListener(onTapOption(this, "Default"));
        mPerformanceMultitasking.setOnClickListener(onTapOption(this, "Conservative"));
    }

    private void refreshRadioButtons(String mode) {
        switch (mode) {
            case "Aggressive":
                mPerformanceRadioGaming.setChecked(true);
                mPerformanceRadioDefault.setChecked(false);
                mPerformanceRadioMultitasking.setChecked(false);
                break;
            case "Conservative":
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

    private View.OnClickListener onTapOption(Context context, String mode) {
        return v -> {
            refreshRadioButtons(mode);
            PerformanceUtils.setPerformanceMode(context, mode);
        };
    }
}