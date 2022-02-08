package io.tensevntysevn.fresh.zest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.AboutPage;
import io.tensevntysevn.fresh.R;

public class AboutActivity extends AppCompatActivity {
    Context mContext;

    @BindView(R.id.zest_about_header)
    AboutPage aboutPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_about);
        ButterKnife.bind(this);

        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfoExp = pm.getPackageInfo("io.tensevntysevn.fresh.framework", 0);
            ApplicationInfo appInfoExp = pm.getApplicationInfo("io.tensevntysevn.fresh.framework", 0);
            String fwVersion = pm.getApplicationLabel(appInfoExp) + " " + packageInfoExp.versionName;
            aboutPage.setOptionalText(fwVersion);
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
}