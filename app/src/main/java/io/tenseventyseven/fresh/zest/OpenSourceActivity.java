package io.tenseventyseven.fresh.zest;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.Preference;
import io.tenseventyseven.fresh.R;

public class OpenSourceActivity extends AppCompatActivity {

    @BindView(R.id.zest_main_toolbar)
    ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_about_open_source);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonTooltip(getString(R.string.abc_action_bar_up_description));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_localization_fragment, new OpenSourceFragment())
                    .commit();
        }
    }

    public static class OpenSourceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_about_open_source, rootKey);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.fresh_background_color_fg, mContext.getTheme()));
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
            return false;
        }
    }
}