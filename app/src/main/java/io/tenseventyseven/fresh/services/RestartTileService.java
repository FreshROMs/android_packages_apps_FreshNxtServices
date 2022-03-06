package io.tensevntysevn.fresh.services;

import de.dlyt.yanndroid.oneui.dialog.AlertDialog;

import android.content.DialogInterface;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.view.ContextThemeWrapper;

import androidx.annotation.RequiresApi;

import io.tensevntysevn.fresh.R;
import io.tensevntysevn.fresh.utils.Tools;

public class RestartTileService extends TileService {

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onClick() {
        AlertDialog qsDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.OneUITheme))
                .setTitle(R.string.qs_dialog_title)
                .setMessage(R.string.qs_dialog_confirmation)
                .setNegativeButton(R.string.qs_dialog_cancel, null)
                .setPositiveButton(R.string.qs_dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Tools.rebootUpdate(getBaseContext());
                    }
                }).create();

        showDialog(qsDialog);
    }


}