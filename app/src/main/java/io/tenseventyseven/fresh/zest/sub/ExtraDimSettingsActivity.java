package io.tenseventyseven.fresh.zest.sub;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import dev.oneuiproject.oneui.layout.SwitchBarLayout;
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

    @BindView(R.id.zest_extra_dim_seekbar_title)
    TextView tvIntensity;

    @BindView(R.id.zest_extra_dim_reboot_textview)
    TextView tvDisableOnReboot;

    private static final int INVERSE_PERCENTAGE_BASE = 100;
    private ColorDisplayManager mColorDisplayManager;
    private ExtraDimSettingsObserver mSettingsObserver;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_extra_dim_settings);
        ButterKnife.bind(this);

        mColorDisplayManager = getSystemService(ColorDisplayManager.class);

        sbLayout.setExpanded(false, false);
        sbLayout.setNavigationButtonTooltip(getString(R.string.abc_action_bar_up_description));
        sbLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(sbLayout.getToolbar());

        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new ExtraDimSettingsObserver(mHandler);

        SwitchBar mExtraDimSwitch = sbLayout.getSwitchBar();
        boolean mExtraDimEnabled = getExtraDimState(mContext);

        mExtraDimSwitch.setChecked(mExtraDimEnabled);
        refreshSubSettings(mExtraDimEnabled);
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

    private void refreshSubSettings(boolean state) {
        tvIntensity.setEnabled(state);
        tvDisableOnReboot.setEnabled(state);
        lyDisableOnReboot.setClickable(state);
        lyDisableOnReboot.setFocusable(state);
        lyDisableOnReboot.setEnabled(state);
        swDisableOnReboot.setEnabled(state);
        skExtraDim.setEnabled(state);
    }

    public static void setExtraDimState(Context context, boolean state) {
        Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_activated", state ? 1 : 0);
    }

    public static boolean getExtraDimState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_activated", 0) > 0;
    }

    public static void setExtraDimRebootState(Context context, boolean state) {
        Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_persist_across_reboots", state ? 1 : 0);
    }

    public static boolean getExtraDimRebootState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_persist_across_reboots", 0) > 0;
    }

    private boolean setExtraDimIntensity(int position) {
        return mColorDisplayManager.setReduceBrightColorsStrength(INVERSE_PERCENTAGE_BASE - position);
    }

    private final class ExtraDimSettingsObserver extends ContentObserver {
        private final Uri SETTING_URI = Settings.Secure.getUriFor("reduce_bright_colors_activated");

        public ExtraDimSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            boolean bool = Settings.Secure.getInt(getContentResolver(), "reduce_bright_colors_activated", 0) == 1;
            sbLayout.getSwitchBar().setChecked(bool);
            refreshSubSettings(bool);
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