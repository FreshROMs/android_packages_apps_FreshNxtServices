package io.tenseventyseven.fresh.ota.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.commonmark.node.Node;

import butterknife.BindView;
import butterknife.ButterKnife;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import androidx.appcompat.widget.SeslProgressBar;
import io.noties.markwon.Markwon;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;

public class DeviceUpdatedActivity extends AppCompatActivity {
    @BindView(R.id.fresh_ota_toolbar_layout)
    ToolbarLayout toolbarLayout;

    @BindView(R.id.fresh_ota_appbar_title)
    TextView mAppBarTitle;
    @BindView(R.id.fresh_ota_appbar_progressbar)
    SeslProgressBar mAppBarProgress;
    @BindView(R.id.fresh_ota_appbar_subtitle)
    TextView mAppBarSubtitle;
    @BindView(R.id.fresh_ota_appbar_remaining)
    TextView mAppBarTimeRemaining;

    @BindView(R.id.fresh_ota_changelog_card)
    MaterialCardView mCardChangelog;
    @BindView(R.id.fresh_ota_app_updates)
    MaterialCardView mCardAppUpdates;

    @BindView(R.id.fresh_ota_changelog)
    TextView mDetailChangelog;
    @BindView(R.id.fresh_ota_app_updates_text)
    TextView mDetailAppUpdates;
    @BindView(R.id.fresh_ota_detail_version)
    TextView mDetailVersion;
    @BindView(R.id.fresh_ota_detail_size)
    TextView mDetailSize;
    @BindView(R.id.fresh_ota_detail_security_patch_level)
    TextView mDetailSecurityPatch;

    @BindView(R.id.fresh_ota_btnbar)
    LinearLayout mButtonBar;
    @BindView(R.id.fresh_ota_btnbar_download)
    LinearLayout mButtonBarDownload;
    @BindView(R.id.fresh_ota_btnbar_install)
    LinearLayout mButtonBarInstall;

    @BindView(R.id.fresh_ota_btn_download)
    MaterialButton mBtnDownload;
    @BindView(R.id.fresh_ota_btn_cancel)
    MaterialButton mBtnCancel;
    @BindView(R.id.fresh_ota_btn_install)
    MaterialButton mBtnInstall;
    @BindView(R.id.fresh_ota_btn_install_later)
    MaterialButton mBtnLater;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fresh_update_screen_activity);
        ButterKnife.bind(this);
        mContext = this;
        ButterKnife.bind(this);

        mContext = this;

        toolbarLayout.setExpanded(false);
        toolbarLayout.setNavigationButtonTooltip(getString(R.string.abc_action_bar_up_description));
        toolbarLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbarLayout.getToolbar());

        mButtonBar.setVisibility(View.GONE);
        mCardChangelog.setVisibility(View.GONE);
        mCardAppUpdates.setVisibility(View.GONE);

        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(mContext);

        mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_detail_no_updates);
        mAppBarSubtitle.setVisibility(View.GONE);
        mAppBarProgress.setVisibility(View.GONE);
        mAppBarTimeRemaining.setVisibility(View.GONE);

        mDetailVersion.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_version), update.getFormattedVersion()));
        mDetailSize.setVisibility(View.GONE);
        mDetailSecurityPatch.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_security_patch_level), update.getSplString()));

    }

}