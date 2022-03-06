package io.tensevntysevn.fresh.zest.sub;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;
import io.tensevntysevn.fresh.R;
import io.tensevntysevn.fresh.ExperienceUtils;

public class VideoBrightnessActivity extends AppCompatActivity {

    Context mContext = this;
    public static Handler UIHandler;
    private static int mAppListCount;

    @BindView(R.id.zest_switchbar_layout)
    SwitchBarLayout sbLayout;
    @BindView(R.id.zest_video_brightness_img)
    ImageView mVbEffectPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_video_brightness);
        ButterKnife.bind(this);

        UIHandler = new Handler(Looper.getMainLooper());
        mContext = this;

        sbLayout.setExpanded(false, false);
        sbLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        sbLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(sbLayout.getToolbar());

        SwitchBar mHDReffectSwitch = sbLayout.getSwitchBar();
        boolean mHDReffectEnabled = ExperienceUtils.isVideoEnhancerEnabled(mContext);

        mHDReffectSwitch.setChecked(mHDReffectEnabled);
        updatePreviewImg(mHDReffectEnabled);

        mHDReffectSwitch.addOnSwitchChangeListener((switchCompat, bool) -> {
            ExperienceUtils.setVideoEnhancerEnabled(mContext, bool);
            updatePreviewImg(bool);
        });

        populateAppList(mContext);
    }

    private void updatePreviewImg(Boolean bool) {
        Drawable previewDisabled = AppCompatResources.getDrawable(mContext, R.drawable.hdr_effect_preview_off);
        Drawable previewEnabled = AppCompatResources.getDrawable(mContext, R.drawable.hdr_effect_preview_on);
        mVbEffectPreview.setImageDrawable(bool ? previewEnabled : previewDisabled);
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.postDelayed(runnable, 2000);
    }

    private void populateAppList(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String[] mCompatibleList = getResources().getStringArray(R.array.hdr_effect_app_compatible_list);
        List<ApplicationInfo> mAppArray = new ArrayList<>();

        LinearLayout semProgress = findViewById(R.id.zest_video_brightness_progress);
        ListView mListview = findViewById(R.id.zest_video_brightness_app_list);

        mListview.setVisibility(View.GONE);
        semProgress.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            for (String s : mCompatibleList) {
                try {
                    PackageManager pm = context.getPackageManager();
                    android.content.pm.ApplicationInfo info = pm.getApplicationInfo(s, 0);
                    Drawable icon = pm.getApplicationIcon(s);
                    String name = pm.getApplicationLabel(info).toString();

                    mAppArray.add(new ApplicationInfo(name, icon));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App not found
                }
            }

            runOnUI(() -> {
                mAppListCount = mAppArray.toArray().length;
                int listHeight = (int) (mAppListCount * 58.4);
                if (listHeight > 300) listHeight = 300;
                int dimensionInDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, listHeight, context.getResources().getDisplayMetrics());

                semProgress.setVisibility(View.GONE);
                mListview.setVisibility(View.INVISIBLE);
                mListview.getLayoutParams().height = dimensionInDp;
                mListview.requestLayout();
                mListview.setVisibility(View.VISIBLE);
                mListview.setAdapter(new AppListAdapter(this, mAppArray));
            });
        });
    }

    public static class AppListAdapter extends ArrayAdapter<ApplicationInfo> {
        List<ApplicationInfo> mAppList;
        Context mContext;

        @BindView(R.id.hdr_effect_app_name)
        TextView mAppName;
        @BindView(R.id.hdr_effect_app_icon)
        ImageView mAppIcon;
        @BindView(R.id.hdr_effect_divider)
        LinearLayout mAppDivider;

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

            if (position == 0) {
                mAppDivider.setVisibility(View.GONE);
            }

            return view;
        }
    }

    public static class ApplicationInfo {
        public String mAppName;
        public Drawable mAppIcon;

        public ApplicationInfo(String appName, Drawable appIcon) {
            mAppName = appName;
            mAppIcon = appIcon;
        }
    }
}