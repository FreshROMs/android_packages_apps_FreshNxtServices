package cf.tenseventyseven.fresh.zest.sub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cf.tenseventyseven.fresh.utils.Maverick;
import de.dlyt.yanndroid.oneui.layout.SwitchBarLayout;
import de.dlyt.yanndroid.oneui.widget.SeekBar;
import de.dlyt.yanndroid.oneui.widget.Switch;
import de.dlyt.yanndroid.oneui.widget.SwitchBar;
import cf.tenseventyseven.fresh.R;

public class UsbSecuritySettingsActivity extends AppCompatActivity {
    private Context mContext;

    @BindView(R.id.zest_usb_security_switchbar_layout)
    SwitchBarLayout sbLayout;

    /*
    @BindView(R.id.zest_usb_security_always_switch)
    Switch swAlwaysEnabled;

    @BindView(R.id.zest_usb_security_always_switch_layout)
    LinearLayout lyAlwaysEnabled;

    @BindView(R.id.zest_usb_security_always_textview)
    TextView tvAlwaysEnabled;
    */

    private UsbSecuritySettingsObserver mSettingsObserver;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.zest_activity_usb_security_settings);
        ButterKnife.bind(this);

        sbLayout.setExpanded(false, false);
        sbLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        sbLayout.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(sbLayout.getToolbar());

        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new UsbSecuritySettingsObserver(mHandler);

        SwitchBar mExtraDimSwitch = sbLayout.getSwitchBar();
        boolean usbSecurityEnabled = Maverick.isMaverickEnabled();

        mExtraDimSwitch.setChecked(usbSecurityEnabled);
        refreshSubSettings(usbSecurityEnabled);
        mExtraDimSwitch.addOnSwitchChangeListener((switchCompat, bool) -> {
            Maverick.setMaverickState(mContext, bool ? Maverick.MaverickState.WHEN_LOCKED : Maverick.MaverickState.OFF);
        });

        /*
        swAlwaysEnabled.setChecked(Maverick.getMaverickAlwaysEnabled(mContext));
        swAlwaysEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> Maverick.setMaverickState(mContext, isChecked ? Maverick.MaverickState.ALWAYS : Maverick.isMaverickEnabled() ? Maverick.MaverickState.WHEN_LOCKED : Maverick.MaverickState.OFF));
        lyAlwaysEnabled.setOnClickListener(v -> swAlwaysEnabled.toggle());
         */
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSettingsObserver != null) {
            mSettingsObserver.setListening(true);
        }
    }

    @Override
    protected void onPause() {
        if (mSettingsObserver != null) {
            mSettingsObserver.setListening(false);
        }

        super.onPause();
    }

    private void refreshSubSettings(boolean state) {
        /*
        tvAlwaysEnabled.setEnabled(state);
        lyAlwaysEnabled.setClickable(state);
        lyAlwaysEnabled.setFocusable(state);
        lyAlwaysEnabled.setEnabled(state);
        swAlwaysEnabled.setEnabled(state);
        */
    }

    private final class UsbSecuritySettingsObserver extends ContentObserver {
        private final Uri SETTING_URI = Settings.Secure.getUriFor(Maverick.MAVERICK_STATE);

        public UsbSecuritySettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            boolean bool = Settings.Secure.getInt(getContentResolver(), Maverick.MAVERICK_STATE, Maverick.MaverickState.OFF) > 0;
            sbLayout.getSwitchBar().setChecked(bool);
            refreshSubSettings(bool);
        }

        public void setListening(boolean z) {
            if (z) {
                getContentResolver().registerContentObserver(this.SETTING_URI, false, this);
            } else {
                getContentResolver().unregisterContentObserver(this);
            }
        }
    }

}