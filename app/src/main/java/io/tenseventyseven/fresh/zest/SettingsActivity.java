package io.tenseventyseven.fresh.zest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.DropDownPreference;
import de.dlyt.yanndroid.oneui.preference.ListPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import de.dlyt.yanndroid.oneui.preference.SwitchPreference;
import de.dlyt.yanndroid.oneui.preference.SwitchPreferenceScreen;
import de.dlyt.yanndroid.oneui.preference.internal.PreferencesRelatedCard;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.services.OverlayService;
import io.tenseventyseven.fresh.utils.Experience;
import io.tenseventyseven.fresh.utils.Performance;
import io.tenseventyseven.fresh.utils.Preferences;
import io.tenseventyseven.fresh.utils.Tools;
import io.tenseventyseven.fresh.zest.sub.ExtraDimSettingsActivity;
import io.tenseventyseven.fresh.zest.sub.FingerprintStyleActivity;
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

            DropDownPreference mDataPreference = findPreference("sb_icon_style_data");
            DropDownPreference mWifiPreference = findPreference("sb_icon_style_wifi");
            DropDownPreference mVoltePreference = findPreference("sb_icon_style_volte");
            Preference mPerformancePreference = findPreference("fs_plus_performance_mode");
            Preference mDeviceResolution = findPreference("fs_device_resolution");
            ListPreference mDeviceScreenRatio = findPreference("fs_device_screen_ratio");
            Preference mVideoBrightness = findPreference("fs_video_brightness");
            Preference mFingerprintAnimation = findPreference("fs_plus_fod_animation_style");

            // Get activated color from attr, so it changes based on the app's theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true);
            @ColorInt int summaryColor = typedValue.data;

            mDataPreference.seslSetSummaryColor(summaryColor);
            mWifiPreference.seslSetSummaryColor(summaryColor);
            mVoltePreference.seslSetSummaryColor(summaryColor);
            mPerformancePreference.seslSetSummaryColor(summaryColor);
            mDeviceResolution.seslSetSummaryColor(summaryColor);
            mDeviceScreenRatio.seslSetSummaryColor(summaryColor);
            mVideoBrightness.seslSetSummaryColor(summaryColor);
            mFingerprintAnimation.seslSetSummaryColor(summaryColor);

            String setResolution = ScreenResolutionActivity.getResolution(mContext);
            String romVersion = Experience.getRomVersion();
            String appVersion = Experience.getAppVersion(mContext);

            // System UI icons
            mDataPreference.setValue(Preferences.getDataConnectionIconPackage(mContext));
            mWifiPreference.setValue(Preferences.getWlanConnectionIconPackage(mContext));
            mVoltePreference.setValue(Preferences.getVolteConnectionIconPackage(mContext));
            mDataPreference.setOnPreferenceChangeListener(this);
            mWifiPreference.setOnPreferenceChangeListener(this);
            mVoltePreference.setOnPreferenceChangeListener(this);

            // Extra Dim
            ((SwitchPreferenceScreen) findPreference("fs_extra_dim")).setChecked(ExtraDimSettingsActivity.getExtraDimState(mContext));
            findPreference("fs_extra_dim").setOnPreferenceChangeListener(this);

            // Screen resolution
            mDeviceResolution.setSummary(setResolution);

            // Screen ratio
            mDeviceScreenRatio.setSummary(mDeviceScreenRatio.getEntry());
            mDeviceScreenRatio.setOnPreferenceChangeListener(this);

            // Fresh and Fresh Services versions
            findPreference("zs_fresh_version").setSummary(romVersion);
            findPreference("zs_fresh_version").setOnPreferenceClickListener(getVersionEgg(mContext));
            findPreference("zs_about_fresh_services").setSummary(appVersion);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();

            switch (prefKey) {
                case "sb_icon_style_data":
                    String[] dataIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String oldDataPackage = Preferences.getDataConnectionIconPackage(mContext);
                    String newDataPackage = newValue.toString();

                    if (!newDataPackage.equals(oldDataPackage)) {
                        Preferences.setDataConnectionIconPackage(mContext, newValue.toString());
                        mExecutor.execute(() -> {
                            if (!dataIconPackages[0].contains(newDataPackage)) {
                                OverlayService.setOverlayState(newDataPackage, true);
                            }

                            if (!dataIconPackages[0].contains(oldDataPackage)) {
                                OverlayService.setOverlayState(oldDataPackage, false);
                            }
                        });
                    }
                    showRebootDialog(mContext);
                    return true;
                case "sb_icon_style_wifi":
                    String[] wifiIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String oldWifiPackage = Preferences.getWlanConnectionIconPackage(mContext);
                    String newWifiPackage = newValue.toString();

                    if (!newWifiPackage.equals(oldWifiPackage)) {
                        Preferences.setWlanConnectionIconPackage(mContext, newValue.toString());
                        mExecutor.execute(() -> {
                            if (!wifiIconPackages[0].contains(newWifiPackage)) {
                                OverlayService.setOverlayState(newWifiPackage, true);
                            }

                            if (!wifiIconPackages[0].contains(oldWifiPackage)) {
                                OverlayService.setOverlayState(oldWifiPackage, false);
                            }
                        });
                    }
                    showRebootDialog(mContext);
                    return true;
                case "sb_icon_style_volte":
                    String[] volteIconPackages = this.getResources().getStringArray(R.array.volte_signal_icon_packages);
                    String oldVoltePackage = Preferences.getVolteConnectionIconPackage(mContext);
                    String newVoltePackage = newValue.toString();

                    if (!newVoltePackage.equals(oldVoltePackage)) {
                        Preferences.setVolteConnectionIconPackage(mContext, newValue.toString());
                        mExecutor.execute(() -> {
                            if (!volteIconPackages[0].contains(newVoltePackage)) {
                                OverlayService.setOverlayState(newVoltePackage, true);
                            }

                            if (!volteIconPackages[0].contains(oldVoltePackage)) {
                                OverlayService.setOverlayState(oldVoltePackage, false);
                            }
                        });
                    }
                    showRebootDialog(mContext);
                    return true;
                case "fs_extra_dim":
                    ExtraDimSettingsActivity.setExtraDimState(mContext, (boolean) newValue);
                    return true;
                case "fs_plus_location_indicator":
                    DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                            "location_indicators_enabled", Boolean.toString((boolean) newValue), true);
                    return true;
                case "fs_device_screen_ratio":
                    handleRatioChange(newValue.toString());
                    return true;
            }
            return false;
        }

        private void handleRatioChange(String newValue) {
            try {
                Object wms = Class.forName("android.view.WindowManagerGlobal").getMethod("getWindowManagerService").invoke(null);
                Class<?> iwm = Class.forName("android.view.IWindowManager");

                int sWidth, sHeight;
                int resIndex = Settings.System.getInt(mContext.getContentResolver(), ScreenResolutionActivity.SCREEN_RESOLUTION, 2);
                String resolution = mContext.getResources().getStringArray(R.array.zest_screen_resolution_setting_values)[resIndex];
                Scanner resScanner = new Scanner(resolution);
                resScanner.useDelimiter(":");
                String wmSize = resScanner.next();

                if (wmSize.equals("reset")) {
                    if (newValue.equals("reset")) {
                        iwm.getMethod("clearForcedDisplaySize", int.class).invoke(wms, Display.DEFAULT_DISPLAY);
                        return;
                    }
                    Point size = new Point();
                    ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
                    sWidth = Math.min(size.x, size.y);
                    sHeight = Math.max(size.x, size.y);
                } else {
                    Scanner scanner = new Scanner(wmSize);
                    scanner.useDelimiter("x");
                    sHeight = scanner.nextInt();
                    sWidth = scanner.nextInt();
                }

                if (!newValue.equals("reset")) {
                    Scanner scanner = new Scanner(newValue);
                    scanner.useDelimiter(":");
                    sHeight = sWidth * scanner.nextInt() / scanner.nextInt();
                }

                iwm.getMethod("setForcedDisplaySize", int.class, int.class, int.class)
                        .invoke(wms, Display.DEFAULT_DISPLAY, sWidth, sHeight);
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            makeRelatedCard(mContext);

            // Video brightness
            findPreference("fs_video_brightness").setSummary(VideoBrightnessActivity.getVideoBrightnessState(mContext) ?
                    mContext.getString(R.string.zest_video_brightness_setting_bright) :
                    mContext.getString(R.string.zest_video_brightness_setting_normal));

            // Fingerprint animation
            String fodAnimationId = Settings.System.getString(mContext.getContentResolver(), "zest_fod_animation_id");
            boolean fodAnimProvisioned = Settings.System.getInt(mContext.getContentResolver(), "fresh_device_fingerprint_provisioned", 0) == 1;

            if (!fodAnimProvisioned || fodAnimationId == null || fodAnimationId.isEmpty())
                fodAnimationId = "default";

            findPreference("fs_plus_fod_animation_style").setSummary(FingerprintStyleActivity.getAnimString(mContext, fodAnimationId));

            // Performance mode
            findPreference("fs_plus_performance_mode").setSummary(Performance.getPerformanceModeString(mContext));

            // Location indicator
            ((SwitchPreference) findPreference("fs_plus_location_indicator")).setChecked(DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                    "location_indicators_enabled", false));
            findPreference("fs_plus_location_indicator").setOnPreferenceChangeListener(this);

            // Disable these settings if app is opened in Samsung DeX
            if (Experience.isDesktopMode(mContext)) {
                findPreference("fs_video_brightness").setEnabled(false);
                findPreference("sb_icon_style_data").setEnabled(false);
                findPreference("sb_icon_style_wifi").setEnabled(false);
                findPreference("sb_icon_style_volte").setEnabled(false);
                findPreference("fs_device_resolution").setEnabled(false);
                findPreference("fs_plus_fod_animation_style").setEnabled(false);
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.item_background_color, mContext.getTheme()));
        }

        private void showRebootDialog(Context context) {
            AlertDialog rebootDialog = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(R.string.zest_customization_reboot_required_title)
                    .setMessage(R.string.zest_customization_reboot_required_description)
                    .setPositiveButton(R.string.zest_customization_reboot_required_btn_ok,
                            (dialog, which) -> Tools.rebootNormal(context))
                    .setNegativeButton(R.string.zest_customization_reboot_required_btn_cancel,
                            (dialog, which) -> {
                                // None
                            }).create();
            rebootDialog.show();
        }

        private void makeRelatedCard(Context context) {
            String advancedTitle = getString(R.string.zest_relative_link_advanced);
            String themesTitle = getString(R.string.zest_relative_link_themes);
            String dressroomTitle = getString(R.string.zest_relative_link_wallpaper_twelve);

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

                if (!Experience.isDesktopMode(mContext)) {
                    mRelatedCard.addButton(themesTitle, themesIntent);
                }

                mRelatedCard.show(this);
            }
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