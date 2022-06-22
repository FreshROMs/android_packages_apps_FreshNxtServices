package io.tenseventyseven.fresh.ota.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ContextThemeWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.view.Toast;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.Utils;
import io.tenseventyseven.fresh.ota.api.UpdateManifest;
import io.tenseventyseven.fresh.utils.Notifications;

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

        Utils.setupNotificationChannels(this);

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
        Utils.scheduleUpdatesCheck(this);
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

        // Cancel existing one-shot check
        Utils.cancelUpdatesCheck(context);

        // Close immediately if we have no internet connection.
        if (!Utils.isDeviceOnline(context)) {
            showErrorToast(context, true);
            Utils.scheduleUpdatesCheck(context);
            handler.postDelayed(UpdateCheckActivity.this::finish, 1500);
            return;
        }

        // Go straight to UpdateAvailableActivity if we have a pending update.
        if (Utils.getUpdateAvailability(context)) {
            Intent intent = new Intent(context, UpdateAvailableActivity.class);
            startActivity(intent);
            UpdateCheckActivity.this.finish();
            return;
        }

        executor.execute(() -> {
            int retCode = UpdateManifest.checkForUpdates(context);

            handler.postDelayed(() -> {
                if (retCode == UpdateManifest.MANIFEST_SUCCESS) {
                    Intent intent = Utils.getUpdateAvailability(context) ? new Intent(context, UpdateAvailableActivity.class) : new Intent(context, DeviceUpdatedActivity.class);
                    startActivity(intent);
                    UpdateCheckActivity.this.finish();
                    return;
                }

                showErrorToast(context, retCode == UpdateManifest.MANIFEST_FAILED_NO_CONNECTION);
                handler.postDelayed(UpdateCheckActivity.this::finish, 1000);
            }, 2000);
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
    }
}