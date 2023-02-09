package io.tenseventyseven.fresh.zest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.AboutPage;
import io.tenseventyseven.fresh.R;

public class AboutActivity extends AppCompatActivity {
    Context mContext;

    @BindView(R.id.zest_about_header)
    AboutPage aboutPage;

    @Override
    @SuppressLint("StringFormatInvalid")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_about);
        ButterKnife.bind(this);

        aboutPage.setToolbarExpandable(true);
        TextView about_optional_text = aboutPage.findViewById(R.id.about_optional_text);
        TextView about_optional_text_2 = new TextView(this);
        about_optional_text_2.setLayoutParams(about_optional_text.getLayoutParams());
        about_optional_text_2.setTextSize(0, this.getResources().getDimension(R.dimen.sesl4_about_secondary_text_size));
        about_optional_text_2.setTextColor(about_optional_text.getCurrentTextColor());
        ((LinearLayout) about_optional_text.getParent()).addView(about_optional_text_2, 3);

        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfoExp = pm.getPackageInfo("io.tensevntysevn.fresh.framework", 0);
            String perfModeVersion = SystemProperties.get("persist.sys.zest.perf_version", "13.0.0.0");

            String expVersionFormat = String.format(getString(R.string.zest_experience_framework_version), packageInfoExp.versionName);
            String perfModeFormat = String.format(getString(R.string.zest_performance_kit_version), perfModeVersion);

            aboutPage.setOptionalText(expVersionFormat);
            about_optional_text_2.setText(perfModeFormat);
        } catch (PackageManager.NameNotFoundException e) {
            aboutPage.setOptionalText(" ");
        }


    }

    public void onTapSourceCode(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "https://github.com/FreshROMs/android_packages_apps_FreshNxtServices";
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public void onTapOpenSource(View v) {
        Intent intent = new Intent(mContext, OpenSourceActivity.class);
        startActivity(intent);
    }

    public void onTapLocalization(View v) {
        Intent intent = new Intent(mContext, LocalizationActivity.class);
        startActivity(intent);
    }
}