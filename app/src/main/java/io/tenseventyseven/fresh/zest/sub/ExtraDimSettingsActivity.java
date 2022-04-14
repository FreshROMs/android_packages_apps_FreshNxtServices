package io.tenseventyseven.fresh.zest.sub;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.ColorDisplayManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.preference.ListPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import de.dlyt.yanndroid.oneui.widget.SeekBar;
import de.dlyt.yanndroid.oneui.widget.Switch;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;
import io.tenseventyseven.fresh.R;

public class ExtraDimSettingsActivity extends AppCompatActivity {
    private Context mContext;

    @BindView(R.id.zest_extra_dim_switchbar_layout)
    SwitchBarLayout sbLayout;

    @BindView(R.id.zest_extra_dim_seekbar)
    SeekBar skExtraDim;

    @BindView(R.id.zest_extra_dim_reboot_switch)
    Switch swDisableOnReboot;

    @BindView(R.id.zest_extra_dim_reboot_switch_layout)
    LinearLayout lyDisableOnReboot;

    private static final int INVERSE_PERCENTAGE_BASE = 100;
    private ColorDisplayManager mColorDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_extra_dim_settings);
        ButterKnife.bind(this);

        mColorDisplayManager = getSystemService(ColorDisplayManager.class);

        sbLayout.setExpanded(false, false);
        sbLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        sbLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(sbLayout.getToolbar());

        SwitchBar mExtraDimSwitch = sbLayout.getSwitchBar();
        boolean mExtraDimEnabled = getExtraDimState(mContext);

        mExtraDimSwitch.setChecked(mExtraDimEnabled);
        mExtraDimSwitch.addOnSwitchChangeListener((switchCompat, bool) -> {
            setExtraDimState(mContext, bool);
        });

        skExtraDim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setExtraDimIntensity(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        swDisableOnReboot.setChecked(getExtraDimRebootState(mContext));
        swDisableOnReboot.setOnCheckedChangeListener((buttonView, isChecked) -> setExtraDimRebootState(mContext, isChecked));
        lyDisableOnReboot.setOnClickListener(v -> swDisableOnReboot.toggle());
    }

    @Override
    protected void onResume() {
        super.onResume();

        skExtraDim.setSeamless(true);
        skExtraDim.setProgress(INVERSE_PERCENTAGE_BASE
                - mColorDisplayManager.getReduceBrightColorsStrength());
        skExtraDim.setMax(INVERSE_PERCENTAGE_BASE
                - ColorDisplayManager.getMinimumReduceBrightColorsStrength(mContext));
        skExtraDim.setMin(INVERSE_PERCENTAGE_BASE
                - ColorDisplayManager.getMaximumReduceBrightColorsStrength(mContext));
    }

    public static void setExtraDimState(Context context, boolean state) {
        Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_activated", state ? 1 : 0);
    }

    public static boolean getExtraDimState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_activated", 0) > 0;
    }

    public static void setExtraDimRebootState(Context context, boolean state) {
        Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_disable_on_boot", state ? 1 : 0);
    }

    public static boolean getExtraDimRebootState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_disable_on_boot", 0) > 0;
    }

    private boolean setExtraDimIntensity(int position) {
        return mColorDisplayManager.setReduceBrightColorsStrength(INVERSE_PERCENTAGE_BASE - position);
    }
}