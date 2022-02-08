package io.tensevntysevn.fresh.zest.sub;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import io.tensevntysevn.fresh.R;
import io.tensevntysevn.fresh.renoir.RenoirReceiver;
import io.tensevntysevn.fresh.renoir.RenoirService;
import io.tensevntysevn.fresh.ExperienceUtils;

public class RenoirSettingsActivity extends AppCompatActivity {

    @BindView(R.id.zest_switchbar_layout)
    SwitchBarLayout mRenoirSwitchBar;

    @BindView(R.id.switch_renoir_lock_screen)
    SwitchMaterial mRenoirLsSwitch;

    @BindView(R.id.switch_renoir_lock_screen_layout)
    LinearLayout renoirLsSwitchLayout;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    boolean mRenoirEnabled;
    boolean mRenoirLsWallpaper;

    Handler handler;

    public static void setLayoutEnabled(View view, boolean enable) {
        view.setEnabled(enable);
        view.setClickable(enable);
        view.setFocusable(enable);
        view.setAlpha(enable ? 1f : 0.7f);
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

    private void updatePreferences() {
        TextView renoirDescription = findViewById(R.id.renoir_description_text);
        SwitchBar renoirSwitchBar = mRenoirSwitchBar.getSwitchBar();

        if (ExperienceUtils.isGalaxyThemeApplied(mContext)) {
            setLayoutEnabled(renoirLsSwitchLayout, false);

            renoirDescription.setText(getString(R.string.zest_renoir_settings_unavailable));
            mRenoirEnabled = false;
            mRenoirLsWallpaper = false;
            renoirSwitchBar.setEnabled(false);
            mRenoirLsSwitch.setEnabled(false);
        } else if (ExperienceUtils.isLsWallpaperUnavailable(mContext)) {
            setLayoutEnabled(renoirLsSwitchLayout, false);

            mRenoirEnabled = RenoirService.getRenoirEnabled(mContext);;
            mRenoirLsWallpaper = false;
            mRenoirLsSwitch.setEnabled(false);
        } else {
            mRenoirEnabled = RenoirService.getRenoirEnabled(mContext);
            mRenoirLsWallpaper = RenoirService.getColorBasedOnLock(mContext);
        }

        renoirSwitchBar.setChecked(mRenoirEnabled);
        setLayoutEnabled(renoirLsSwitchLayout, mRenoirEnabled);
        mRenoirLsSwitch.setEnabled(mRenoirEnabled);
        mRenoirLsSwitch.setChecked(mRenoirLsWallpaper);

        renoirSwitchBar.addOnSwitchChangeListener((buttonView, isChecked) -> {
            if (!(isChecked == mRenoirEnabled)) {
                buttonView.setChecked(isChecked);
                mRenoirEnabled = isChecked;

                renoirSwitchBar.setProgressBarVisible(true);
                mRenoirSwitchBar.setEnabled(false);
                setLayoutEnabled(renoirLsSwitchLayout, false);

                RenoirService.setRenoirEnabled(mContext, isChecked);

                handler.postDelayed(() -> {
                    renoirSwitchBar.setProgressBarVisible(false);
                    mRenoirSwitchBar.setEnabled(true);
                    setLayoutEnabled(renoirLsSwitchLayout, !isChecked);
                    mRenoirLsSwitch.setEnabled(!isChecked);
                }, 1500);
            }
        });

        mRenoirLsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!(isChecked == mRenoirLsWallpaper)) {
                mRenoirLsWallpaper = isChecked;
                renoirSwitchBar.setProgressBarVisible(true);
                mRenoirSwitchBar.setEnabled(false);
                setLayoutEnabled(renoirLsSwitchLayout, false);
                RenoirService.setColorBasedOnLock(mContext, isChecked);
                RenoirReceiver.runRenoir(mContext);

                handler.postDelayed(() -> {
                    renoirSwitchBar.setProgressBarVisible(false);
                    mRenoirSwitchBar.setEnabled(true);
                    setLayoutEnabled(renoirLsSwitchLayout, true);
                }, 1500);
            }
        });
    }
}