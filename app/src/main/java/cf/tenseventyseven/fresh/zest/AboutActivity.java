package cf.tenseventyseven.fresh.zest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import cf.tenseventyseven.fresh.utils.Experience;
import de.dlyt.yanndroid.oneui.layout.AboutPage;
import cf.tenseventyseven.fresh.R;

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
        aboutPage.setOptionalText(" ");
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