package io.tenseventyseven.fresh.ota.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;

import org.commonmark.node.Node;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.ProgressBar;
import io.noties.markwon.Markwon;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.api.UpdateDownload;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;

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

    private static Context mContext;
    private static SoftwareUpdate mUpdate;
    private Fetch mFetch;
    private final FetchListener mFetchListener = new FetchListener() {
        @Override
        public void onWaitingNetwork(@NonNull Download download) {

        }

        @Override
        public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {
            mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_downloading);
            mAppBarSubtitle.setVisibility(View.GONE);

            mBtnDownload.setText(R.string.fresh_ota_changelog_btn_pause);
            mBtnDownload.setOnClickListener(v -> pauseUpdate());
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_DOWNLOADING);
            updateAppBar();
        }

        @Override
        public void onResumed(@NonNull Download download) {
            mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_downloading);
            mBtnDownload.setText(R.string.fresh_ota_changelog_btn_pause);
            mBtnDownload.setOnClickListener(v -> pauseUpdate());
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_DOWNLOADING);
            updateAppBar();
        }

        @Override
        public void onRemoved(@NonNull Download download) {

        }

        @Override
        public void onQueued(@NonNull Download download, boolean b) {

        }

        @Override
        public void onProgress(@NonNull Download download, long eta, long speed) {
            mProgress = download.getProgress();
            mAppBarProgress.setProgress(mProgress, true);
            updateTimeLeft(eta);
            mEta = eta;
        }

        @Override
        public void onPaused(@NonNull Download download) {
            mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_paused);
            mBtnDownload.setText(R.string.fresh_ota_changelog_btn_resume);
            mBtnDownload.setOnClickListener(v -> resumeUpdate());
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_PAUSED);
            updateAppBar();
        }

        @Override
        public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_FAILED);
            updateAppBar();
        }

        @Override
        public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {

        }

        @Override
        public void onDeleted(@NonNull Download download) {

        }

        @Override
        public void onCompleted(@NonNull Download download) {
            CurrentSoftwareUpdate.setOtaDownloadId(mContext, 0);
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_COMPLETE);
            updateButtonBar();
            updateAppBar();
            if (mFetchListener != null)
                mFetch.removeListener(mFetchListener);
        }

        @Override
        public void onCancelled(@NonNull Download download) {
        }

        @Override
        public void onAdded(@NonNull Download download) {

        }
    };

    private long mEta;
    private int mProgress;

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

        mUpdate = CurrentSoftwareUpdate.getSoftwareUpdate(mContext);
        mFetch = Fetch.Impl.getInstance(UpdateDownload.getFetchConfig(mContext));

        mAppBarProgress.setVisibility(View.GONE);
        mAppBarTimeRemaining.setVisibility(View.GONE);

        final Node node = markwon.parse(mUpdate.getChangelog());
        final Spanned markdown = markwon.render(node);

        markwon.setParsedMarkdown(mDetailChangelog, markdown);
        mDetailVersion.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_version), mUpdate.getFormattedVersion()));
        mDetailSize.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_size), mUpdate.getFileSizeFormat()));
        mDetailSecurityPatch.setText(String.format("%s %s", getString(R.string.fresh_ota_changelog_detail_security_patch_level), mUpdate.getSplString()));

        StringBuilder appList = new StringBuilder();
        JSONArray jArray;

        try {
            jArray = new JSONArray(mUpdate.getUpdatedApps());
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

        mBtnCancel.setOnClickListener(v -> cancelUpdate());
        updateButtonBar();
        updateAppBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppBarProgress.setProgress(CurrentSoftwareUpdate.getOtaDownloadProgress(mContext));
        updateTimeLeft(CurrentSoftwareUpdate.getOtaDownloadEta(mContext));
        mFetch.addListener(mFetchListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        CurrentSoftwareUpdate.setOtaDownloadEta(mContext, mEta);
        CurrentSoftwareUpdate.setOtaDownloadProgress(mContext, mProgress);
        mFetch.removeListener(mFetchListener);
    }

    private void startDownload() {
        final Request request = new Request(mUpdate.getFileUrl(), UpdateUtils.getUpdatePackageFile().getPath());
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.UNMETERED);
        CurrentSoftwareUpdate.setOtaDownloadId(mContext, request.getId());

        mFetch.enqueue(request, updatedRequest -> {
            CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_DOWNLOADING);
            updateAppBar();
            updateButtonBar();
        }, error -> {

        });
    }

    private void updateTimeLeft(long eta) {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeLeft = String.format(Locale.getDefault(), "%s %s",
                mContext.getString(R.string.fresh_ota_changelog_appbar_downloading_time_left),
                (eta == 0) ? "00:00:00" : formatter.format(new Date(eta)));

        mAppBarTimeRemaining.setText(timeLeft);
    }

    private void downloadUpdate() {
        mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_downloading);
        mAppBarSubtitle.setVisibility(View.GONE);
        mAppBarProgress.setVisibility(View.VISIBLE);
        mAppBarTimeRemaining.setVisibility(View.VISIBLE);

        startDownload();
    }

    private void pauseUpdate() {
        mFetch.pause(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
        mFetch.removeListener(mFetchListener);
    }

    private void resumeUpdate() {
        mFetch.resume(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
        updateAppBar();
        updateButtonBar();
    }

    private void cancelUpdate() {
        mFetch.cancel(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
        CurrentSoftwareUpdate.setOtaDownloadState(mContext, CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_CANCELLED);
        updateAppBar();
        updateButtonBar();
    }

    private void installUpdate() {

    }

    private void installUpdateLater() {

    }

    private void updateAppBar() {
        int currentState = CurrentSoftwareUpdate.getOtaDownloadState(mContext);
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(mContext);

        switch (currentState) {
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_COMPLETE:
                mAppBarTitle.setText(mContext.getString(R.string.fresh_ota_changelog_appbar_install, update.getVersionName(), update.getReleaseType()));
                mAppBarSubtitle.setVisibility(View.VISIBLE);
                mAppBarProgress.setVisibility(View.GONE);
                mAppBarTimeRemaining.setVisibility(View.GONE);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_FAILED:
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_NOT_STARTED:
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_CANCELLED:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_detail);
                mAppBarSubtitle.setVisibility(View.VISIBLE);
                mAppBarProgress.setVisibility(View.GONE);
                mAppBarTimeRemaining.setVisibility(View.GONE);
                mAppBarProgress.setProgress(0, true);
                updateTimeLeft(0);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_DOWNLOADING:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_downloading);
                mAppBarSubtitle.setVisibility(View.GONE);
                mAppBarProgress.setVisibility(View.VISIBLE);
                mAppBarTimeRemaining.setVisibility(View.VISIBLE);
                mAppBarProgress.setProgress(0, true);
                updateTimeLeft(0);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_PAUSED:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_paused);
                mAppBarSubtitle.setVisibility(View.GONE);
                mAppBarProgress.setVisibility(View.VISIBLE);
                mAppBarTimeRemaining.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateButtonBar() {
        int currentState = CurrentSoftwareUpdate.getOtaDownloadState(mContext);

        if (currentState == CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_COMPLETE) {
            mButtonBarInstall.setVisibility(View.VISIBLE);
            mButtonBarDownload.setVisibility(View.GONE);
        } else {
            mButtonBarInstall.setVisibility(View.GONE);
            mButtonBarDownload.setVisibility(View.VISIBLE);
        }

        switch (currentState) {
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_COMPLETE:
                mBtnInstall.setEnabled(true);
                mBtnLater.setEnabled(true);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_FAILED:
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_CANCELLED:
                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_download);
                mBtnDownload.setOnClickListener(v -> downloadUpdate());
                mBtnCancel.setOnClickListener(v -> onBackPressed());
                mBtnCancel.setEnabled(true);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_DOWNLOADING:
                mBtnDownload.setOnClickListener(v -> pauseUpdate());
                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_pause);
                mBtnCancel.setOnClickListener(v -> cancelUpdate());
                mBtnCancel.setEnabled(true);
                break;
            case CurrentSoftwareUpdate.OTA_DOWNLOAD_STATE_PAUSED:
                mBtnDownload.setOnClickListener(v -> resumeUpdate());
                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_resume);
                mBtnCancel.setOnClickListener(v -> cancelUpdate());
                mBtnCancel.setEnabled(true);
                break;
        }
    }
}