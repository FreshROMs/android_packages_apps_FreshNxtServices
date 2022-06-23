package io.tenseventyseven.fresh.ota.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ContextThemeWrapper;

import org.json.JSONException;
import org.lineageos.updater.download.DownloadClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.view.Toast;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.UpdateCheckJobService;
import io.tenseventyseven.fresh.ota.UpdateNotifications;
import io.tenseventyseven.fresh.ota.UpdateUtils;
import io.tenseventyseven.fresh.ota.api.UpdateManifest;

public class UpdateCheckActivity extends AppCompatActivity {
    @BindView(R.id.fresh_ota_check_toolbar_layout)
    ToolbarLayout toolbarLayout;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fresh_update_check_activity);
        ButterKnife.bind(this);

        toolbarLayout.setExpanded(false);
        toolbarLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbarLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbarLayout.getToolbar());

        UpdateNotifications.setupNotificationChannels(this);

        // But check permissions first - download will be started in the callback
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        // Check for permissions before initiating update task.
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            checkForUpdates(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Re-schedule updates check.
        UpdateCheckJobService.setupCheckJob(this);
        UpdateCheckActivity.this.finish();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE && (grantResults.length <= 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            showPermissionDialog();
        }
    }

    private void checkForUpdates(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        UpdateCheckJobService.cancelCheckJob(context);
        executor.execute(() -> {
            String server = UpdateManifest.whichServiceReachable(context);

            if (server == null) {
                handler.post(() -> {
                    showErrorToast(context, true);

                    // Re-schedule
                    UpdateCheckJobService.setupCheckJob(context);
                    handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
                });
                return;
            }

            if (UpdateManifest.getUpdateAvailability(context)) {
                UpdateNotifications.showNewUpdateNotification(context);
                UpdateCheckJobService.setupCheckJob(context);

                handler.post(() -> {
                    Intent intent = new Intent(context, UpdateAvailableActivity.class);
                    startActivity(intent);
                    UpdateCheckActivity.this.finish();
                });
                return;
            }

            UpdateNotifications.showOngoingCheckNotification(context);
            final File json = new File(context.getFilesDir(), UpdateManifest.MANIFEST_FILE_NAME);

            DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
                @Override
                public void onFailure(boolean cancelled) {
                    handler.post(() -> {
                        showErrorToast(context, false);

                        // Re-schedule
                        UpdateCheckJobService.setupCheckJob(context);
                        handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
                    });
                }

                @Override
                public void onResponse(DownloadClient.Headers headers) {
                }

                @Override
                public void onSuccess() {
                    try {
                        if (!json.exists() || !UpdateManifest.parseManifest(context, json)) {
                            handler.postDelayed(() -> {
                                showErrorToast(context, false);
                                handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
                            }, 2000);
                            return;
                        }

                        UpdateCheckJobService.setupCheckJob(context);

                        handler.postDelayed(() -> {
                            boolean isUpdateAvailable = UpdateManifest.getUpdateAvailability(context);

                            UpdateUtils.setSettingAppBadge(context, isUpdateAvailable);
                            UpdateUtils.setLastCheckedDate(context);

                            UpdateNotifications.cancelOngoingCheckNotification(context);
                            Intent intent = new Intent(context,
                                    isUpdateAvailable ? UpdateAvailableActivity.class : DeviceUpdatedActivity.class);
                            startActivity(intent);
                            UpdateCheckActivity.this.finish();
                        }, 2000);
                    } catch (IOException | JSONException e) {
                        handler.post(() -> {
                            showErrorToast(context, false);

                            // Re-schedule
                            UpdateCheckJobService.setupCheckJob(context);
                            handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
                        });
                    }
                }
            };

            try {
                DownloadClient downloadClient = new DownloadClient.Builder()
                        .setUrl(server)
                        .setDestination(json)
                        .setDownloadCallback(callback)
                        .build();
                downloadClient.start();
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    showErrorToast(context, false);

                    // Re-schedule
                    UpdateCheckJobService.setupCheckJob(context);
                    handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
                });
            }
        });
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.OneUITheme))
                .setTitle(R.string.fresh_permissions_storage_title)
                .setMessage(R.string.fresh_permissions_storage_description)
                .setPositiveButton(R.string.qs_dialog_ok, (dialog, which) -> UpdateCheckActivity.this.finish())
                .create()
                .show();
    }

    private static void showErrorToast(Context context, boolean noConnection) {
        Toast.makeText(context, noConnection ? R.string.fresh_ota_toast_check_failed_offline : R.string.fresh_ota_toast_check_failed_generic, Toast.LENGTH_SHORT).show();
        UpdateNotifications.cancelOngoingCheckNotification(context);
    }
}