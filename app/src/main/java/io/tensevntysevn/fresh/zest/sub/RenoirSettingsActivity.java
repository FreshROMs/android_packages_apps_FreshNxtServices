package io.tensevntysevn.fresh.zest.sub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.ClassicColorPickerDialog;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.widget.Switch;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;
import io.tensevntysevn.fresh.ExperienceUtils;
import io.tensevntysevn.fresh.R;
import io.tensevntysevn.fresh.renoir.RenoirService;

public class RenoirSettingsActivity extends AppCompatActivity {

    @BindView(R.id.zest_switchbar_layout)
    SwitchBarLayout mRenoirSwitchBar;

    @BindView(R.id.switch_renoir_lock_screen)
    Switch mRenoirLsSwitch;

    @BindView(R.id.switch_renoir_lock_screen_layout)
    LinearLayout renoirLsSwitchLayout;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    boolean mRenoirEnabled;
    boolean mRenoirLsWallpaper;

    boolean mRenoirCc;
    Switch mRenoirCcSwitch;
    LinearLayout renoirCcSwitchLayout;
    View renoirCcPickerView;
    int mRenoirCustomColor;

    Handler handler;

    public static void setLayoutEnabled(View view, boolean enable) {
        view.setEnabled(enable);
        view.setClickable(enable);
        view.setFocusable(enable);
        view.setAlpha(enable ? 1f : 0.7f);

        for (int i = 0; i < ((LinearLayout) view).getChildCount(); i++)
            ((LinearLayout) view).getChildAt(i).setEnabled(enable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_renoir_settings);
        ButterKnife.bind(this);

        mContext = this;
        handler = new Handler(Looper.getMainLooper());

        mRenoirEnabled = RenoirService.getRenoirEnabled(mContext);
        mRenoirLsWallpaper = RenoirService.getColorBasedOnLock(mContext);

        mRenoirCc = RenoirService.getColorBasedOnCustom(mContext);
        mRenoirCustomColor = RenoirService.getColorForBasedOnCustom(mContext);
        mRenoirCcSwitch = findViewById(R.id.switch_renoir_custom_color);
        renoirCcSwitchLayout = findViewById(R.id.switch_renoir_custom_color_layout);
        renoirCcPickerView = findViewById(R.id.custom_color_circle);

        mRenoirSwitchBar.setExpanded(false, false);
        mRenoirSwitchBar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        mRenoirSwitchBar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(mRenoirSwitchBar.getToolbar());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        updatePreferences();
        super.onResume();
    }

    public void toggleRenoirLsSwitch(View v) {
        mRenoirLsSwitch.toggle();
    }

    public void toggleRenoirCcSwitch(View v) {
        mRenoirCcSwitch.toggle();
    }

    private void updatePreferences() {
        TextView renoirDescription = findViewById(R.id.renoir_description_text);
        SwitchBar renoirSwitchBar = mRenoirSwitchBar.getSwitchBar();

        if (ExperienceUtils.isGalaxyThemeApplied(mContext)) {
            renoirDescription.setText(getString(R.string.zest_renoir_settings_unavailable));
            renoirSwitchBar.setEnabled(false);

            mRenoirEnabled = false;
            mRenoirLsWallpaper = false;
            mRenoirCc = false;

            setLayoutEnabled(renoirLsSwitchLayout, false);
            setLayoutEnabled(renoirCcSwitchLayout, false);
        } else if (ExperienceUtils.isLsWallpaperUnavailable(mContext)) {
            mRenoirEnabled = RenoirService.getRenoirEnabled(mContext);
            mRenoirLsWallpaper = false;
            mRenoirCc = RenoirService.getColorBasedOnCustom(mContext);

            setLayoutEnabled(renoirLsSwitchLayout, false);
            setLayoutEnabled(renoirCcSwitchLayout, mRenoirEnabled);
        } else {
            mRenoirEnabled = RenoirService.getRenoirEnabled(mContext);
            mRenoirLsWallpaper = RenoirService.getColorBasedOnLock(mContext);
            mRenoirCc = RenoirService.getColorBasedOnCustom(mContext);

            setLayoutEnabled(renoirLsSwitchLayout, mRenoirEnabled && !mRenoirCc);
            setLayoutEnabled(renoirCcSwitchLayout, mRenoirEnabled && !mRenoirLsWallpaper);
        }
        mRenoirCustomColor = RenoirService.getColorForBasedOnCustom(mContext);

        renoirSwitchBar.setChecked(mRenoirEnabled);
        mRenoirLsSwitch.setChecked(mRenoirLsWallpaper);
        mRenoirCcSwitch.setChecked(mRenoirCc);
        renoirCcPickerView.setVisibility(mRenoirCc ? View.VISIBLE : View.GONE);

        renoirSwitchBar.addOnSwitchChangeListener((buttonView, isChecked) -> {
            if (isChecked != mRenoirEnabled) {
                mRenoirEnabled = isChecked;

                renoirSwitchBar.setProgressBarVisible(true);
                renoirSwitchBar.setEnabled(false);
                setLayoutEnabled(renoirLsSwitchLayout, false);
                setLayoutEnabled(renoirCcSwitchLayout, false);
                RenoirService.setRenoirEnabled(mContext, isChecked);

                handler.postDelayed(() -> {
                    renoirSwitchBar.setProgressBarVisible(false);
                    renoirSwitchBar.setEnabled(true);
                    setLayoutEnabled(renoirLsSwitchLayout, isChecked && !mRenoirCc);
                    setLayoutEnabled(renoirCcSwitchLayout, isChecked && !mRenoirLsWallpaper);
                }, 1500);
            }
        });

        mRenoirLsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!(isChecked == mRenoirLsWallpaper)) {
                if (isChecked) mRenoirCcSwitch.setChecked(false);
                mRenoirLsWallpaper = isChecked;
                renoirSwitchBar.setProgressBarVisible(true);
                renoirSwitchBar.setEnabled(false);
                setLayoutEnabled(renoirLsSwitchLayout, false);
                setLayoutEnabled(renoirCcSwitchLayout, false);
                RenoirService.setColorBasedOnLock(mContext, isChecked);

                handler.postDelayed(() -> {
                    renoirSwitchBar.setProgressBarVisible(false);
                    renoirSwitchBar.setEnabled(true);
                    setLayoutEnabled(renoirLsSwitchLayout, !mRenoirCc);
                    setLayoutEnabled(renoirCcSwitchLayout, !isChecked);
                }, 1500);
            }
        });

        mRenoirCcSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!(isChecked == mRenoirCc)) {
                if (isChecked) mRenoirLsSwitch.setChecked(false);
                mRenoirCc = isChecked;
                renoirSwitchBar.setProgressBarVisible(true);
                renoirSwitchBar.setEnabled(false);
                setLayoutEnabled(renoirLsSwitchLayout, false);
                setLayoutEnabled(renoirCcSwitchLayout, false);
                RenoirService.setColorBasedOnCustom(mContext, isChecked, mRenoirCustomColor);

                renoirCcPickerView.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                handler.postDelayed(() -> {
                    renoirSwitchBar.setProgressBarVisible(false);
                    renoirSwitchBar.setEnabled(true);
                    setLayoutEnabled(renoirCcSwitchLayout, !mRenoirLsWallpaper);
                    setLayoutEnabled(renoirLsSwitchLayout, !isChecked);
                }, 1500);
            }
        });

        //GradientDrawable circleDrawable = (GradientDrawable) ((RippleDrawable) renoirCcPickerView.getForeground()).getDrawable(0);
        GradientDrawable circleDrawable = (GradientDrawable) renoirCcPickerView.getForeground();
        circleDrawable.setColor(mRenoirCustomColor);

        ClassicColorPickerDialog mColorPickerDialog = new ClassicColorPickerDialog(mContext, i -> {
            renoirSwitchBar.setProgressBarVisible(true);
            renoirSwitchBar.setEnabled(false);

            mRenoirCustomColor = i;
            circleDrawable.setColor(ColorStateList.valueOf(mRenoirCustomColor));
            RenoirService.setColorBasedOnCustom(mContext, mRenoirCcSwitch.isChecked(), mRenoirCustomColor);

            handler.postDelayed(() -> {
                renoirSwitchBar.setProgressBarVisible(false);
                renoirSwitchBar.setEnabled(true);
            }, 1500);
        }, mRenoirCustomColor);

        renoirCcPickerView.setOnClickListener(v -> mColorPickerDialog.show());
    }
}