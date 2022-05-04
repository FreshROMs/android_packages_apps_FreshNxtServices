package io.tenseventyseven.fresh.zest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.tenseventyseven.fresh.zest.AboutActivityClass;
import io.tenseventyseven.fresh.R;

public class AboutActivity extends AppCompatActivity {
    Context mContext;

    @BindView(R.id.zest_about_header)
    AboutActivityClass aboutPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_about);
        ButterKnife.bind(this);

        aboutPage.setToolbarExpandable(true);

        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfoExp = pm.getPackageInfo("io.tensevntysevn.fresh.framework", 0);
            String perfModeVersion = SystemProperties.get("persist.sys.zest.perf_version", "13.0.0.0");

            String expVersionFormat = String.format(getString(R.string.zest_experience_framework_version), packageInfoExp.versionName);
            String perfModeFormat = String.format(getString(R.string.zest_performance_kit_version), perfModeVersion);

            aboutPage.setOptionalText(expVersionFormat);
            aboutPage.setOptionalText2(perfModeFormat);
        } catch (PackageManager.NameNotFoundException e) {
            aboutPage.setOptionalText(" ");
        }


    }

    public void onTapSourceCode(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "https://github.com/TenSeventy7/FreshNxtServices";
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