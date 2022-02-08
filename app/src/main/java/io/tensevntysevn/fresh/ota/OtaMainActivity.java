package io.tensevntysevn.fresh.ota;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.Preference;
import io.tensevntysevn.fresh.R;

public class OtaMainActivity extends AppCompatActivity {

    private static final String TITLE_TAG = "otaActivity";
    @BindView(R.id.fresh_ota_toolbar)
    ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_main);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fresh_ota_frame_layout, new OtaMainFragment())
                    .commit();
        } else {
            toolbar.setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        toolbar.inflateToolbarMenu(R.menu.settings_search);
        toolbar.setOnToolbarMenuItemClickListener(this::onOptionsItemSelected);
        return true;
    }

    private boolean onOptionsItemSelected(de.dlyt.yanndroid.oneui.menu.MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.zest_settings_shortcut) {
            ComponentName cn = new ComponentName("com.android.settings.intelligence", "com.android.settings.intelligence.search.SearchActivity");
            Intent intent = new Intent();
            intent.setComponent(cn);
            this.startActivity(intent);
        }
        return true;
    }

    public static class OtaMainFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;
        private static ExecutorService mExecutor;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
            mExecutor = Executors.newCachedThreadPool();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.fresh_firmware_update, rootKey);
        }

        @Override
        public void onStart() {
            super.onStart();
            findPreference("ota_last_update").setEnabled(false);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return true;
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.item_background_color, mContext.getTheme()));
        }
    }

}