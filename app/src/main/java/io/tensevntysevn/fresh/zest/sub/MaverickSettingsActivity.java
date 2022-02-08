package io.tensevntysevn.fresh.zest.sub;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.preference.ListPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;
import io.tensevntysevn.fresh.R;

public class MaverickSettingsActivity extends AppCompatActivity {
    private Context mContext;
    static private final String MAVERICK_MODE = "maverick_usb_protect_mode";
    static private final String MAVERICK_STATE = "maverick_usb_protect_state";

    @BindView(R.id.zest_maverick_switchbar_layout)
    SwitchBarLayout sbLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_maverick_settings);
        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_maverick_setting_layout, new MaverickSettingsFragment())
                    .commit();
        }

        sbLayout.setExpanded(false, false);
        sbLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        sbLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(sbLayout.getToolbar());

        SwitchBar mMaverickSwitch = sbLayout.getSwitchBar();
        boolean mMaverickEnabled = getMaverickState(mContext);

        mMaverickSwitch.setChecked(mMaverickEnabled);
        mMaverickSwitch.addOnSwitchChangeListener((switchCompat, bool) -> {
            setMaverickState(mContext, bool);
            MaverickSettingsFragment.setSummary(mContext);
        });
    }

    private static void setMaverickMode(Context context, int mode) {
        Settings.System.putInt(context.getContentResolver(), MAVERICK_MODE, mode);
    }

    public static void setMaverickState(Context context, boolean state) {
        Settings.System.putInt(context.getContentResolver(), MAVERICK_STATE, state ? 1 : 0);
    }

    public static int getMaverickMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(), MAVERICK_MODE, 1);
    }

    public static boolean getMaverickState(Context context) {
        return Settings.System.getInt(context.getContentResolver(), MAVERICK_STATE, 0) > 0;
    }

    public static class MaverickSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;
        private static ListPreference mMaverickLockPref;
        private static String[] mvOptions;

        @Override
        public void onStart() {
            super.onStart();
            mMaverickLockPref = (ListPreference) findPreference("mv_lock_options");
            mvOptions = mContext.getResources().getStringArray(R.array.zest_maverick_security_setting_options);

            // Get activated color from attr, so it changes based on the app's theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true);
            @ColorInt int summaryColor = typedValue.data;
            mMaverickLockPref.seslSetSummaryColor(summaryColor);
            mMaverickLockPref.setOnPreferenceChangeListener(this);
            setSummary(mContext);
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_activity_maverick_settings, rootKey);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();
            if (prefKey.equals(mMaverickLockPref.getKey())) {
                int setValue = Integer.parseInt((String) newValue);
                MaverickSettingsActivity.setMaverickMode(mContext, setValue);
                setSummary(mContext);
            }
            return false;
        }

        private static void setSummary(Context context) {
            int mode = MaverickSettingsActivity.getMaverickMode(context);
            boolean state = MaverickSettingsActivity.getMaverickState(context);
            mMaverickLockPref.setSummary(mvOptions[mode - 1]);
            mMaverickLockPref.setValue(String.valueOf(mode));
            mMaverickLockPref.setEnabled(state);
        }
    }
}