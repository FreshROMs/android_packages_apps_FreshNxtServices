package io.tenseventyseven.fresh.ota.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchErrorUtils;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2core.DownloadBlock;

import org.commonmark.node.Node;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.ProgressBar;
import io.noties.markwon.Markwon;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.SoftwareUpdate;
import io.tenseventyseven.fresh.ota.UpdateNotifications;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.api.UpdateDownload;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.utils.Notifications;

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
            if (download.getProgress() > 0) {
                mProgress = download.getProgress();
                mEta = download.getEtaInMilliSeconds();
                updateState(SoftwareUpdate.OTA_INSTALL_STATE_LOST_CONNECTION);
            }
        }

        @Override
        public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING);
        }

        @Override
        public void onResumed(@NonNull Download download) {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING);
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
            mEta = eta;

            mAppBarProgress.setProgress(mProgress, true);
            updateSubtitleText(mEta, speed);
        }

        @Override
        public void onPaused(@NonNull Download download) {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_PAUSED);
        }

        @Override
        public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
            Toast.makeText(mContext, R.string.fresh_ota_toast_failed_download_generic, Toast.LENGTH_SHORT).show();
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_FAILED);
        }

        @Override
        public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {

        }

        @Override
        public void onDeleted(@NonNull Download download) {
        }

        @Override
        public void onCompleted(@NonNull Download download) {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING);
        }

        @Override
        public void onCancelled(@NonNull Download download) {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_CANCELLED);
        }

        @Override
        public void onAdded(@NonNull Download download) {

        }
    };

    private long mEta;
    private int mProgress;

    private static Dialog mLoadingDialog = null;

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

        mAppBarProgress.setVisibility(View.GONE);
        mAppBarTimeRemaining.setVisibility(View.GONE);

        String changelog = mUpdate.getChangelog();

        if (changelog.isEmpty())
            mCardChangelog.setVisibility(View.GONE);
        else {
            final Node node = markwon.parse(changelog);
            final Spanned markdown = markwon.render(node);

            markwon.setParsedMarkdown(mDetailChangelog, markdown);
        }

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
    }

    @Override
    public void onResume() {
        super.onResume();

        mAppBarProgress.setProgress(CurrentSoftwareUpdate.getOtaDownloadProgress(mContext));
        updateSubtitleText(CurrentSoftwareUpdate.getOtaDownloadEta(mContext), 0);
        mBtnInstall.setOnClickListener(v -> installUpdate());
        mBtnLater.setOnClickListener(v -> cancelUpdateExit());

        if (mFetch == null || mFetch.isClosed()) {
            mFetch = UpdateDownload.getFetchInstance(mContext);
            mFetch.addListener(mFetchListener);
        }

        setupLoadingDialog();
        updateState(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        int state = CurrentSoftwareUpdate.getOtaState(mContext);
        CurrentSoftwareUpdate.setOtaDownloadEta(mContext, mEta);
        CurrentSoftwareUpdate.setOtaDownloadProgress(mContext, mProgress);

        if (!mFetch.isClosed())
            mFetch.removeListener(mFetchListener);

        if (state == SoftwareUpdate.OTA_INSTALL_STATE_FAILED ||
                state == SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION ||
                state == SoftwareUpdate.OTA_INSTALL_STATE_CANCELLED ||
                state == SoftwareUpdate.OTA_INSTALL_STATE_NOT_STARTED ||
                state == SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED ||
                state == SoftwareUpdate.OTA_INSTALL_STATE_UNKNOWN)
            UpdateDownload.tryStopService(mContext);
    }

    private void updateSubtitleText(long eta, long speed) {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeLeft = String.format(Locale.getDefault(), "%s %s",
                mContext.getString(R.string.fresh_ota_changelog_appbar_downloading_time_left),
                (eta == 0) ? "00:00:00" : formatter.format(new Date(eta)));
        String speedString = UpdateUtils.getFormattedSpeed(speed);

        mAppBarTimeRemaining.setText(String.format("%s \u2022 %s", timeLeft, speedString));
    }

    private void downloadUpdateWithWarning() {
        if (!UpdateUtils.isDeviceOnline(mContext)) {
            Toast.makeText(mContext, R.string.network_connect_is_not_stable, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show warning if user is downloading from a data connection
        if (!UpdateUtils.isConnectionUnmetered(mContext)) {
            AlertDialog warningDialog = new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setTitle(R.string.fresh_ota_download_block_dialog_title)
                    .setMessage(R.string.fresh_ota_download_block_dialog_description)
                    .setPositiveButton(R.string.fresh_ota_download_block_dialog_later, (dialog, which) -> {})
                    .setNegativeButton(R.string.fresh_ota_download_block_dialog_download, (dialog, which) -> downloadUpdate()).create();
            warningDialog.show();
        } else
            downloadUpdate();
    }

    private void downloadUpdate() {
        Notifications.cancelNotification(mContext, UpdateNotifications.NOTIFICATION_AVAILABLE_UPDATE_ID);
        Notifications.cancelNotification(mContext, UpdateNotifications.NOTIFICATION_POST_UPDATE_ID);

        UpdateDownload.startService(mContext);
        mFetch = UpdateDownload.getFetchInstance(mContext);
        mFetch.addListener(mFetchListener);

        UpdateDownload.downloadUpdate(mContext, success -> {
            updateState(SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING);
        }, error -> {
            Throwable th = error.getThrowable();

            if (th == null) {
                Toast.makeText(mContext, R.string.fresh_ota_toast_failed_download_network, Toast.LENGTH_SHORT).show();
                updateState(SoftwareUpdate.OTA_INSTALL_STATE_FAILED);
                return;
            }

            Error e = FetchErrorUtils.getErrorFromThrowable(th);
            switch (e) {
                case FILE_ALLOCATION_FAILED:
                case FILE_NOT_CREATED:
                case NO_STORAGE_SPACE:
                    Toast.makeText(mContext, R.string.fresh_ota_toast_failed_no_space, Toast.LENGTH_SHORT).show();
                    break;
                case EMPTY_RESPONSE_FROM_SERVER:
                case CONNECTION_TIMED_OUT:
                case HTTP_NOT_FOUND:
                case UNKNOWN_HOST:
                    Toast.makeText(mContext, R.string.fresh_ota_toast_failed_download_server, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(mContext, R.string.fresh_ota_toast_failed_download_network, Toast.LENGTH_SHORT).show();
                    break;
            }

            updateState(SoftwareUpdate.OTA_INSTALL_STATE_FAILED);
        });
    }

    private void pauseUpdate() {
        Toast.makeText(mContext, R.string.fresh_ota_changelog_appbar_paused, Toast.LENGTH_SHORT).show();
        mFetch.pause(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
    }

    private void resumeUpdate() {
        mFetch.resume(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
    }

    private void cancelUpdate() {
        Toast.makeText(mContext, R.string.fresh_ota_notification_download_cancelled, Toast.LENGTH_SHORT).show();
        mFetch.cancel(CurrentSoftwareUpdate.getOtaDownloadId(mContext));
    }

    private void cancelUpdateExit() {
        onBackPressed();
    }

    private void installUpdate() {
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setCancelable(false)
                .setTitle(R.string.fresh_ota_main_title)
                .setMessage(R.string.fresh_ota_install_dialog_description)
                .setPositiveButton(R.string.fresh_ota_changelog_btn_install, (d, w) -> { UpdateUtils.installUpdate(mContext); })
                .setNegativeButton(R.string.fresh_ota_changelog_btn_cancel, (d, w) -> {})
                .create();
        dialog.show();

    }

    private void installUpdateLater() {

    }

    private void updateState(int state) {
        SoftwareUpdate update = CurrentSoftwareUpdate.getSoftwareUpdate(mContext);
        if (state == 0)
            state = CurrentSoftwareUpdate.getOtaState(mContext);

        if (state != SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING)
            CurrentSoftwareUpdate.setOtaState(mContext, state);

        // Show button bar based on state
        mButtonBarInstall.setVisibility(state == SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED ? View.VISIBLE : View.GONE);
        mButtonBarDownload.setVisibility(state == SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED ? View.GONE : View.VISIBLE);

        if (state == SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION) {
            Toast.makeText(mContext, R.string.fresh_ota_notification_verification_failed_description, Toast.LENGTH_SHORT).show();
        }

        switch (state) {
            case SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADED:
                mLoadingDialog.hide();
                mAppBarTitle.setText(mContext.getString(R.string.fresh_ota_changelog_appbar_install,
                        update.getVersionName(), update.getFormattedReleaseType()));
                mAppBarSubtitle.setVisibility(View.VISIBLE);
                mAppBarSubtitle.setText(R.string.fresh_ota_changelog_appbar_install_summary);
                mAppBarProgress.setVisibility(View.GONE);
                mAppBarTimeRemaining.setVisibility(View.GONE);

                mAppBarProgress.setProgress(100, true);
                updateSubtitleText(0, 0);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_download);
                mBtnDownload.setOnClickListener(v -> downloadUpdateWithWarning());
                mBtnCancel.setOnClickListener(v -> onBackPressed());
                mBtnInstall.setEnabled(true);
                mBtnLater.setEnabled(true);

                UpdateDownload.tryStopService(this);
                break;
            case SoftwareUpdate.OTA_INSTALL_STATE_FAILED:
            case SoftwareUpdate.OTA_INSTALL_STATE_NOT_STARTED:
            case SoftwareUpdate.OTA_INSTALL_STATE_CANCELLED:
            case SoftwareUpdate.OTA_INSTALL_STATE_FAILED_VERIFICATION:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_detail);
                mAppBarSubtitle.setVisibility(View.VISIBLE);
                mAppBarSubtitle.setText(R.string.fresh_ota_check_for_updates_summary);
                mAppBarProgress.setVisibility(View.GONE);
                mAppBarTimeRemaining.setVisibility(View.GONE);

                mAppBarProgress.setProgress(0, true);
                updateSubtitleText(0, 0);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_download);
                mBtnDownload.setOnClickListener(v -> downloadUpdateWithWarning());
                mBtnCancel.setOnClickListener(v -> cancelUpdateExit());
                break;
            case SoftwareUpdate.OTA_INSTALL_STATE_DOWNLOADING:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_downloading);
                mAppBarSubtitle.setVisibility(View.GONE);
                mAppBarSubtitle.setText(R.string.fresh_ota_check_for_updates_summary);
                mAppBarProgress.setVisibility(View.VISIBLE);
                mAppBarTimeRemaining.setVisibility(View.VISIBLE);
                mAppBarProgress.setProgress(0, true);
                updateSubtitleText(0, 0);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_pause);
                mBtnDownload.setOnClickListener(v -> pauseUpdate());
                mBtnCancel.setOnClickListener(v -> cancelUpdate());
                break;
            case SoftwareUpdate.OTA_INSTALL_STATE_LOST_CONNECTION:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_waiting);
                mAppBarSubtitle.setVisibility(View.GONE);
                mAppBarSubtitle.setText(R.string.fresh_ota_check_for_updates_summary);
                mAppBarProgress.setVisibility(View.VISIBLE);
                mAppBarTimeRemaining.setVisibility(View.VISIBLE);
                mAppBarProgress.setProgress(mProgress, true);
                updateSubtitleText(mEta, 0);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_pause);
                mBtnDownload.setOnClickListener(v -> pauseUpdate());
                mBtnCancel.setOnClickListener(v -> cancelUpdate());
                showDownloadLostDialog();
                break;
            case SoftwareUpdate.OTA_INSTALL_STATE_PAUSED:
                mAppBarTitle.setText(R.string.fresh_ota_changelog_appbar_paused);
                mAppBarSubtitle.setVisibility(View.GONE);
                mAppBarProgress.setVisibility(View.VISIBLE);
                mAppBarTimeRemaining.setVisibility(View.VISIBLE);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_resume);
                mBtnDownload.setOnClickListener(v -> resumeUpdate());
                mBtnCancel.setOnClickListener(v -> cancelUpdate());
                break;
            case SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING:
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                mLoadingDialog.show();
                mAppBarTitle.setText(mContext.getString(R.string.fresh_ota_changelog_appbar_install,
                        update.getVersionName(), update.getReleaseType()));
                mAppBarSubtitle.setVisibility(View.VISIBLE);
                mAppBarSubtitle.setText(R.string.fresh_ota_changelog_appbar_install_summary);
                mAppBarProgress.setVisibility(View.GONE);
                mAppBarTimeRemaining.setVisibility(View.GONE);

                mAppBarProgress.setProgress(100, true);
                updateSubtitleText(0, 0);

                mBtnDownload.setText(R.string.fresh_ota_changelog_btn_download);
                mBtnDownload.setOnClickListener(v -> downloadUpdateWithWarning());
                mBtnCancel.setOnClickListener(v -> onBackPressed());
                mBtnInstall.setEnabled(false);
                mBtnLater.setEnabled(false);

                executor.execute(() -> {
                    while (CurrentSoftwareUpdate.getOtaState(mContext) == SoftwareUpdate.OTA_INSTALL_STATE_VERIFYING) {
                        SystemClock.sleep(500);
                    }
                    handler.post(() -> updateState(0));
                });
        }
    }

    private void setupLoadingDialog() {
        final View layout = getLayoutInflater().inflate(R.layout.dialog_full_loading, null);

        mLoadingDialog = new Dialog(mContext, R.style.LargeProgressDialog);
        mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setContentView(layout);

        Window window = mLoadingDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        window.setAttributes(wlp);
    }

    private void showDownloadLostDialog() {
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setCancelable(false)
                .setTitle(R.string.fresh_ota_download_block_dialog_title)
                .setMessage(R.string.fresh_ota_download_block_dialog_description)
                .setPositiveButton(R.string.qs_dialog_ok, (d, w) -> {onBackPressed();})
                .create();
        dialog.show();
    }
}