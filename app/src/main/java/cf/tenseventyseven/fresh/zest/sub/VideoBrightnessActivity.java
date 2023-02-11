package cf.tenseventyseven.fresh.zest.sub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.reflect.SeslBaseReflector;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import cf.tenseventyseven.fresh.utils.Experience;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.Switch;
import cf.tenseventyseven.fresh.R;

public class VideoBrightnessActivity extends AppCompatActivity {

    Context mContext = this;
    public static Handler UIHandler;

    @BindView(R.id.zest_video_brightness_toolbar)
    ToolbarLayout toolbar;
    @BindView(R.id.zest_video_brightness_summary)
    TextView mTextViewSummary;

    @BindView(R.id.zest_video_brightness_radio_normal)
    RadioButton mRadioNormal;
    @BindView(R.id.zest_video_brightness_radio_bright)
    RadioButton mRadioBright;

    @BindView(R.id.zest_video_brightness_text_normal)
    TextView mTextViewNormal;
    @BindView(R.id.zest_video_brightness_text_bright)
    TextView mTextViewBright;

    @BindView(R.id.zest_video_brightness_radio_layout_normal)
    LinearLayout mLayoutNormal;
    @BindView(R.id.zest_video_brightness_radio_layout_bright)
    LinearLayout mLayoutBright;
    @BindView(R.id.zest_video_brightness_apps_list)
    LinearLayout mLayoutList;

    @BindView(R.id.zest_video_brightness_progress)
    LinearLayout semProgress;
    @BindView(R.id.zest_video_brightness_app_list)
    ListView mListview;

    List<ApplicationInfo> mAppArray;
    private VideoBrightnessSettingsObserver mSettingsObserver;
    private Handler mHandler;

    Boolean mFinishedPopulating = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_video_brightness);
        ButterKnife.bind(this);

        UIHandler = new Handler(Looper.getMainLooper());
        mContext = this;

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        refreshRadioButtons(getVideoBrightnessState(mContext));

        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new VideoBrightnessSettingsObserver(mHandler);

        mLayoutNormal.setOnTouchListener(this::onTapRadioButton);
        mLayoutBright.setOnTouchListener(this::onTapRadioButton);

        mLayoutNormal.setOnClickListener(v -> onToggle(false));
        mLayoutBright.setOnClickListener(v -> onToggle(true));

        populateAppList();
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

    private void onToggle(boolean enabled) {
        setVideoBrightnessState(mContext, enabled);
        refreshRadioButtons(enabled);
        showList(enabled, false);
    }

    private void refreshRadioButtons(boolean state) {
        if (state) {
            mRadioBright.setChecked(true);
            mTextViewBright.setTextColor(getColor(R.color.primary_color));
            mTextViewBright.setTypeface(Typeface.DEFAULT_BOLD);

            mRadioNormal.setChecked(false);
            mTextViewNormal.setTextColor(getColor(R.color.sesl4_primary_text));
            mTextViewNormal.setTypeface(Typeface.DEFAULT);
            mTextViewSummary.setText(R.string.zest_video_brightness_summary_bright);
        } else {
            mRadioNormal.setChecked(true);
            mTextViewNormal.setTextColor(getColor(R.color.primary_color));
            mTextViewNormal.setTypeface(Typeface.DEFAULT_BOLD);

            mRadioBright.setChecked(false);
            mTextViewBright.setTextColor(getColor(R.color.sesl4_primary_text));
            mTextViewBright.setTypeface(Typeface.DEFAULT);
            mTextViewSummary.setText(R.string.zest_video_brightness_summary_normal);
        }
    }

    private boolean onTapRadioButton(View v, MotionEvent event) {
        int id = v.getId();
        TextView tv;
        RadioButton rd;

        if (id == R.id.zest_video_brightness_radio_layout_bright) {
            tv = mTextViewBright;
            rd = mRadioBright;
        } else {
            tv = mTextViewNormal;
            rd = mRadioNormal;
        }

        float alpha;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                alpha = 0.6f;
                tv.setAlpha(alpha);
                rd.setAlpha(alpha);
                return false;
            case MotionEvent.ACTION_UP:
                alpha = 1.0f;
                tv.setAlpha(alpha);
                rd.setAlpha(alpha);
                v.callOnClick();
                return false;
            case MotionEvent.ACTION_CANCEL:
                alpha = 1.0f;
                tv.setAlpha(alpha);
                rd.setAlpha(alpha);
                return false;
            default:
                return false;
        }
    }

    public static boolean getVideoBrightnessState(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "hdr_effect", 0) == 1;
    }

    public static void setVideoBrightnessState(Context context, Boolean bool) {
        Settings.System.putInt(context.getContentResolver(), "hdr_effect", bool ? 1 : 0);
    }

    private void populateAppList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String resPackage = Experience.FRAMEWORK_PACKAGE;
        Resources resArray;

        PackageManager pm = getPackageManager();

        try {
            resArray = pm.getResourcesForApplication(resPackage);
        } catch (PackageManager.NameNotFoundException e) {
            resPackage = "android";
            try {
                resArray = pm.getResourcesForApplication(resPackage);
            } catch (PackageManager.NameNotFoundException ex) {
                ex.printStackTrace();
                return;
            }
        }

        String[] mCompatibleList = resArray.getStringArray(resArray.getIdentifier("config_Video_App_Launcher",
                "array", resPackage));
        mAppArray = new ArrayList<>();

        mListview.setVisibility(View.GONE);
        semProgress.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            for (String s : mCompatibleList) {
                try {
                    android.content.pm.ApplicationInfo info = pm.getApplicationInfo(s, 0);
                    Drawable icon = pm.getApplicationIcon(s);
                    String name = pm.getApplicationLabel(info).toString();

                    mAppArray.add(new ApplicationInfo(s, name, icon));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App not found
                }
            }
        });

        mFinishedPopulating = true;
        showList(getVideoBrightnessState(this), true);
    }

    private void showList(boolean state, boolean justLaunched) {
        mLayoutList.setVisibility(state ? View.VISIBLE : View.GONE);
        semProgress.setVisibility(state ? !justLaunched && mFinishedPopulating ? View.GONE : View.VISIBLE : View.GONE);

        UIHandler.postDelayed(() -> {
            int mAppListCount = mAppArray.toArray().length;
            int listHeight = mAppListCount * 60;
            int dimensionInDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, listHeight, getResources().getDisplayMetrics());

            if (justLaunched) semProgress.setVisibility(View.GONE);
            mListview.setVisibility(View.INVISIBLE);
            mListview.getLayoutParams().height = dimensionInDp;
            mListview.requestLayout();
            mListview.setVisibility(View.VISIBLE);
            mListview.setAdapter(new AppListAdapter(this, mAppArray));
        }, !justLaunched && mFinishedPopulating ? 0 : 2000);
    }

    @SuppressLint({"PrivateApi", "WrongConstant"})
    private static void setAppEnabled(Context context, String appName, Boolean state) throws Exception {
        Method method = SeslBaseReflector.getDeclaredMethod(
                "com.samsung.android.displaysolution.SemDisplaySolutionManager",
                "setVideoEnhancerSettingState",
                String.class, Integer.TYPE);

        if (method == null) {
            return;
        }

        SeslBaseReflector.invoke(context.getSystemService("DisplaySolution"), method, appName, state ? 1 : 0);
    }

    private static boolean getAppEnabled(Context context, String appName) {
        Method method = SeslBaseReflector.getDeclaredMethod(
                "com.samsung.android.displaysolution.SemDisplaySolutionManager",
                "getVideoEnhancerSettingState",
                String.class);

        if (method == null) {
            return false;
        }

        @SuppressLint("WrongConstant")
        Object result = SeslBaseReflector.invoke(context.getSystemService("DisplaySolution"), method, appName);

        if (result == null)
            return false;

        return Integer.parseInt(result.toString()) == 1;
    }

    public static class AppListAdapter extends ArrayAdapter<ApplicationInfo> {
        List<ApplicationInfo> mAppList;
        Context mContext;

        @BindView(R.id.hdr_effect_app_name)
        TextView mAppName;
        @BindView(R.id.hdr_effect_app_icon)
        ImageView mAppIcon;
        @BindView(R.id.hdr_effect_switch)
        Switch mAppSwitch;
        @BindView(R.id.hdr_effect_layout)
        LinearLayout mLayout;

        public AppListAdapter(Context context, List<ApplicationInfo> appList) {
            super(context, R.layout.zest_activity_video_brightness_list_item, appList);
            this.mAppList = appList;
            this.mContext = context;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final ApplicationInfo item = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.zest_activity_video_brightness_list_item, parent, false);
            }
            ButterKnife.bind(this, view);

            assert item != null;
            mAppName.setText(item.mAppName);
            mAppIcon.setImageDrawable(item.mAppIcon);

            item.mSwitch = mAppSwitch;
            mAppSwitch.setChecked(getAppEnabled(mContext, item.mPackageName));
            mAppSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    setAppEnabled(mContext, item.mPackageName, isChecked);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            mLayout.setOnClickListener(v -> item.mSwitch.toggle());
            return view;
        }
    }

    public static class ApplicationInfo {
        public String mAppName;
        public Drawable mAppIcon;
        public String mPackageName;
        public Switch mSwitch;

        public ApplicationInfo(String packageName, String appName, Drawable appIcon) {
            mAppName = appName;
            mAppIcon = appIcon;
            mPackageName = packageName;
        }
    }

    private final class VideoBrightnessSettingsObserver extends ContentObserver {
        private final Uri SETTING_URI = Settings.System.getUriFor("hdr_effect");

        public VideoBrightnessSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            boolean bool = Settings.System.getInt(getContentResolver(), "hdr_effect", 0) == 1;
            refreshRadioButtons(bool);
            showList(bool, false);
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