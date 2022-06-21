package io.tenseventyseven.fresh.ota.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.view.Toast;
import io.tenseventyseven.fresh.R;
import io.tenseventyseven.fresh.ota.Utils;
import io.tenseventyseven.fresh.ota.api.UpdateManifest;
import io.tenseventyseven.fresh.ota.db.CurrentSoftwareUpdate;
import io.tenseventyseven.fresh.zest.OpenSourceActivity;

public class UpdateCheckActivity extends AppCompatActivity {
    private static Context mContext;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fresh_update_check_activity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        mContext = this;

        // But check permissions first - download will be started in the callback
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        executor.execute(() -> {
            int retCode = UpdateManifest.downloadManifest(mContext);

            if (retCode == UpdateManifest.MANIFEST_SUCCESS)
                retCode = UpdateManifest.parseManifest(mContext);

            int finalRetCode = retCode;

            handler.post(() -> {
                if (finalRetCode == UpdateManifest.MANIFEST_SUCCESS) {
                    Intent intent = Utils.getUpdateAvailability(mContext) ? new Intent(mContext, UpdateAvailableActivity.class) : new Intent(mContext, DeviceUpdatedActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(mContext,
                            finalRetCode == UpdateManifest.MANIFEST_FAILED_NO_CONNECTION ?
                                    R.string.fresh_ota_toast_check_failed_offline : R.string.fresh_ota_toast_check_failed_generic,
                            Toast.LENGTH_SHORT).show();
                }

                this.finish();
            });
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE && (grantResults.length <= 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.OneUITheme))
                    .setTitle(R.string.fresh_permissions_storage_title)
                    .setMessage(R.string.fresh_permissions_storage_description)
                    .setPositiveButton(R.string.qs_dialog_ok, (dialog, which) -> UpdateCheckActivity.this.finish())
                    .create()
                    .show();
        }
    }
}