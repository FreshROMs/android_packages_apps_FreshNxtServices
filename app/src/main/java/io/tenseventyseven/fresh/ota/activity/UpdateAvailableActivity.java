package io.tenseventyseven.fresh.ota.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.commonmark.node.Node;
import org.json.JSONArray;
import org.json.JSONException;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.ProgressBar;
import io.noties.markwon.Markwon;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.ota.db.LastSoftwareUpdate;

public class UpdateAvailableActivity extends AppCompatActivity {
    @BindView(R.id.fresh_ota_toolbar_layout)
    ToolbarLayout toolbarLayout;

    @BindView(R.id.fresh_ota_appbar_title)
    TextView mAppBarTitle;
    @BindView(R.id.fresh_ota_appbar_progressbar)
    ProgressBar mAppBarProgress;
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
        final Markwon markwon = Markwon.create(mContext);

        toolbarLayout.setExpanded(false);
        toolbarLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbarLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbarLayout.getToolbar());

        mButtonBarInstall.setVisibility(View.GONE);

        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(mContext);

        mAppBarProgress.setVisibility(View.GONE);
        mAppBarTimeRemaining.setVisibility(View.GONE);

        final Node node = markwon.parse(update.getChangelog());
        final Spanned markdown = markwon.render(node);

        markwon.setParsedMarkdown(mDetailChangelog, markdown);
        mDetailVersion.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_version), update.getFormattedVersion()));
        mDetailSize.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_size), update.getFileSizeFormat()));
        mDetailSecurityPatch.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_security_patch_level), update.getSplString()));

        StringBuilder appList = new StringBuilder();
        JSONArray jArray;

        try {
            jArray = new JSONArray(update.getUpdatedApps());
            if (jArray.length() == 0) {
                mCardAppUpdates.setVisibility(View.GONE);
            } else {
                for (int i = 0; i < jArray.length(); i++){
                    appList.append("- ").append(jArray.getString(i)).append("\n");
                }

                final Node appNode = markwon.parse(appList.toString());
                final Spanned appMarkdown = markwon.render(appNode);
                markwon.setParsedMarkdown(mDetailAppUpdates, appMarkdown);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mCardAppUpdates.setVisibility(View.GONE);
        }
    }

}