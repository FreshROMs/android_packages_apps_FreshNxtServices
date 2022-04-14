package io.tenseventyseven.fresh.zest;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import io.tenseventyseven.fresh.services.ExtraDimTileService;
import io.tenseventyseven.fresh.zest.sub.ExtraDimSettingsActivity;

public class QsTileHandler extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        if (!intent.getAction().equals(TileService.ACTION_QS_TILE_PREFERENCES)) {
            finish();
            return;
        }
        ComponentName cn = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        if (cn != null) {
            if (cn.getClassName().equals(ExtraDimTileService.class.getName())) {
                Intent settings = new Intent(this, ExtraDimSettingsActivity.class);
                startActivity(settings);
            }
        }
        finish();
    }
}