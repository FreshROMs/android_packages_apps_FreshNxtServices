package io.tenseventyseven.fresh.zest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.preference.ListPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.SwitchPreferenceScreen;
import de.dlyt.yanndroid.oneui.preference.internal.PreferencesRelatedCard;
import io.tenseventyseven.fresh.ExperienceUtils;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.services.OverlayService;
import io.tenseventyseven.fresh.utils.Preferences;
import io.tenseventyseven.fresh.zest.sub.ExtraDimSettingsActivity;
import io.tenseventyseven.fresh.zest.sub.MaverickSettingsActivity;
import io.tenseventyseven.fresh.zest.sub.ScreenResolutionActivity;
import io.tenseventyseven.fresh.zest.sub.VideoBrightnessActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String TITLE_TAG = "settingsActivityTitle";

    @BindView(R.id.zest_main_toolbar)
    ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_main);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_settings_main_layout, new ZestMainFragment())
                    .commit();
        } else {
            toolbar.setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        toolbar.inflateToolbarMenu(R.menu.settings_search);
        toolbar.setOnToolbarMenuItemClickListener(this::onOptionsItemSelected);
        return true;
    }

    private boolean onOptionsItemSelected(de.dlyt.yanndroid.oneui.menu.MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.zest_settings_shortcut) {
            ComponentName cn = new ComponentName("com.android.settings.intelligence", "com.android.settings.intelligence.search.SearchActivity");
            Intent intent = new Intent();
            intent.setComponent(cn);
            this.startActivity(intent);
        }
        return true;
    }

    public static class ZestMainFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;
        private static ExecutorService mExecutor;
        private PreferencesRelatedCard mRelatedCard;

        private static boolean mBackground = false;
        private static Handler mHandler;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
            mExecutor = Executors.newCachedThreadPool();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_activity_main_preferences, rootKey);
        }


        @Override
        public void onStart() {
            super.onStart();

            // Get activated color from attr, so it changes based on the app's theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true);
            @ColorInt int summaryColor = typedValue.data;

            findPreference("sb_icon_style_data").seslSetSummaryColor(summaryColor);
            findPreference("sb_icon_style_wifi").seslSetSummaryColor(summaryColor);
            findPreference("fs_device_resolution").seslSetSummaryColor(summaryColor);

            String setResolution = getResolution(mContext);
            String romVersion = getRomVersion();
            String appVersion = getAppVersion(mContext);
            boolean vbEnabled = VideoBrightnessActivity.getVideoBrightnessState(mContext);
            boolean mvEnabled = MaverickSettingsActivity.getMaverickState(mContext);
            Preference.OnPreferenceClickListener easterEgg = getVersionEgg(mContext);

            // System UI icons
            setIconSummary();
            findPreference("sb_icon_style_data").setOnPreferenceChangeListener(this);
            findPreference("sb_icon_style_wifi").setOnPreferenceChangeListener(this);

            // Video preferences
            ((SwitchPreferenceScreen) findPreference("fs_video_brightness")).setChecked(vbEnabled);
            findPreference("fs_video_brightness").setOnPreferenceChangeListener(this);

            // Extra Dim
            ((SwitchPreferenceScreen) findPreference("fs_extra_dim")).setChecked(ExtraDimSettingsActivity.getExtraDimState(mContext));
            findPreference("fs_extra_dim").setOnPreferenceChangeListener(this);

            /*
            // USB protection
            ((SwitchPreferenceScreen) findPreference("fs_plus_usb_security")).setChecked(mvEnabled);
            findPreference("fs_plus_usb_security").setOnPreferenceChangeListener(this);
             */

            // Screen resolution
            findPreference("fs_device_resolution").setSummary(setResolution);

            // Fresh and Fresh Services versions
            findPreference("zs_fresh_version").setSummary(romVersion);
            findPreference("zs_fresh_version").setOnPreferenceClickListener(easterEgg);
            findPreference("zs_about_fresh_services").setSummary(appVersion);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();
            Handler mHandler = new Handler(Looper.getMainLooper());

            switch (prefKey) {
                case "sb_icon_style_data":
                case "sb_icon_style_wifi":
                    String[] dataIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String[] wlanIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String oldPackage;

                    if (prefKey.equals("sb_icon_style_data")) {
                        oldPackage = Preferences.getDataConnectionIconPackage(mContext);
                        if (oldPackage == null)
                            oldPackage = dataIconPackages[0];
                    } else {
                        oldPackage = Preferences.getWlanConnectionIconPackage(mContext);
                        if (oldPackage == null)
                            oldPackage = wlanIconPackages[0];
                    }

                    String newPackage = newValue.toString();

                    if (prefKey.equals("sb_icon_style_data") && !newPackage.equals(oldPackage)) {
                        Preferences.setDataConnectionIconPackage(mContext, newValue.toString());
                        String finalDataOldPackage = oldPackage;
                        mExecutor.execute(() -> {
                            if (!dataIconPackages[0].contains(newPackage)) {
                                OverlayService.setOverlayState(newPackage, true);
                            }

                            if (!dataIconPackages[0].contains(finalDataOldPackage)) {
                                OverlayService.setOverlayState(finalDataOldPackage, false);
                            }
                        });
                    } else if (prefKey.equals("sb_icon_style_wifi") && !newPackage.equals(oldPackage)) {
                        Preferences.setWlanConnectionIconPackage(mContext, newValue.toString());
                        String finalOldWlanPackage = oldPackage;
                        mExecutor.execute(() -> {
                            if (!wlanIconPackages[0].contains(newPackage)) {
                                OverlayService.setOverlayState(newPackage, true);
                            }

                            if (!wlanIconPackages[0].contains(finalOldWlanPackage)) {
                                OverlayService.setOverlayState(finalOldWlanPackage, false);
                            }
                        });

                    }

                    setIconSummary();
                    return true;
                case "fs_video_brightness":
                    VideoBrightnessActivity.setVideoBrightnessState(mContext, (boolean) newValue);
                    return true;
                case "fs_extra_dim":
                    ExtraDimSettingsActivity.setExtraDimState(mContext, (boolean) newValue);
                    return true;
                    /*
                case "fs_plus_usb_security":
                    MaverickSettingsActivity.setMaverickState(mContext, (boolean) newValue);
                    return true;

                     */
            }
            return false;
        }

        @Override
        public void onResume() {
            super.onResume();
            makeRelatedCard(mContext);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.item_background_color, mContext.getTheme()));
        }

        private void makeRelatedCard(Context context) {
            String advancedTitle = getString(R.string.zest_relative_link_advanced);
            String themesTitle = getString(R.string.zest_relative_link_themes);
            String dressroomTitle = getString(R.string.zest_relative_link_wallpaper);

            View.OnClickListener advancedIntent = v -> {
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$UsefulFeatureMainActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };
            View.OnClickListener themesIntent = v -> {
                ComponentName cn = new ComponentName("com.samsung.android.themestore", "com.samsung.android.themestore.activity.MainActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };
            View.OnClickListener dressroomIntent = v -> {
                ComponentName cn = new ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.settings.WallpaperSettingActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };

            if (mRelatedCard == null) {
                mRelatedCard = createRelatedCard(context);
                mRelatedCard.addButton(advancedTitle, advancedIntent)
                        .addButton(dressroomTitle, dressroomIntent);

                if (!ExperienceUtils.isDesktopMode(mContext)) {
                    mRelatedCard.addButton(themesTitle, themesIntent);
                }

                mRelatedCard.show(this);
            }
        }

        private static String getRomVersion() {
            String romPropVersion = ExperienceUtils.getProp("ro.fresh.version");
            String romPropBuild = ExperienceUtils.getProp("ro.fresh.build.version");
            String romPropBranch = ExperienceUtils.getProp("ro.fresh.build.branch");
            String romPropBuildDate = ExperienceUtils.getProp("ro.fresh.build.date");

            String romVersionBranch = "";
            String buildDate = ExperienceUtils.getProp("ro.system.build.date");

            if (!romPropBuildDate.equals("")) {
                buildDate = ExperienceUtils.getProp("ro.fresh.build.date");
            }

            if (!romPropBranch.isEmpty()) {
                romVersionBranch = romPropBranch.substring(0, 1).toUpperCase() +
                        romPropBranch.substring(1).toLowerCase();
            }

            String romVersion = romPropVersion + " " + romVersionBranch + " " + "(" + romPropBuild + ")";

            return romVersion + "\n" + buildDate;
        }

        private static String getAppVersion(Context context) {
            String appVersion;
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String versionName = packageInfo.versionName;
                @SuppressWarnings("deprecation") int versionCode = packageInfo.versionCode;
                appVersion = versionName + " (" + versionCode + ")";
            } catch (PackageManager.NameNotFoundException e) {
                appVersion = "Unknown";
            }

            return appVersion;
        }

        private static String getResolution(Context context) {
            String[] mResolutionValues = context.getResources().getStringArray(R.array.zest_screen_resolution_setting_main_summary);
            int setResolution = ScreenResolutionActivity.getResolutionInt(context);
            return mResolutionValues[setResolution];
        }

        private void setIconSummary() {
            String[] suiValues = getSuiValues(mContext);
            ((ListPreference) findPreference("sb_icon_style_data")).setValue(suiValues[0]);
            ((ListPreference) findPreference("sb_icon_style_wifi")).setValue(suiValues[1]);
            CharSequence valData = ((ListPreference) findPreference("sb_icon_style_data")).getEntry();
            CharSequence valWlan = ((ListPreference) findPreference("sb_icon_style_wifi")).getEntry();

            findPreference("sb_icon_style_data").setSummary(valData);
            findPreference("sb_icon_style_wifi").setSummary(valWlan);
        }

        private static String[] getSuiValues(Context context) {
            String[] dataIconPackages = context.getResources().getStringArray(R.array.data_connection_icon_packages);
            String[] wlanIconPackages = context.getResources().getStringArray(R.array.wlan_signal_icon_packages);
            String sDataIcon = Preferences.getDataConnectionIconPackage(context);
            String sWlanIcon = Preferences.getWlanConnectionIconPackage(context);

            if (sDataIcon == null)
                sDataIcon = dataIconPackages[0];
            if (sWlanIcon == null)
                sWlanIcon = wlanIconPackages[0];

            return new String[]{sDataIcon, sWlanIcon};
        }

        private static Preference.OnPreferenceClickListener getVersionEgg(Context context) {
            long[] mHits = new long[3];

            return listener -> {
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                    String url = "https://www.youtube.com/watch?v=MCYY9ZLLg1w"; // get ready for ch4nge *wink*
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(intent);
                }
                return true;
            };
        }
    }
}