package cf.tenseventyseven.fresh.zest;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import butterknife.BindView;
import butterknife.ButterKnife;
import cf.tenseventyseven.fresh.R;
import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class LocalizationActivity extends AppCompatActivity {

    @BindView(R.id.zest_main_toolbar)
    ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_about_localization);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
//        setSupportActionBar(toolbar.getToolbar());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_localization_fragment, new OpenSourceFragment())
                    .commit();
        }
    }

    public static class OpenSourceFragment extends PreferenceFragmentCompat {
        private Context mContext;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_about_localization, rootKey);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.sesl_fragment_fgcolor, mContext.getTheme()));
        }
    }
}