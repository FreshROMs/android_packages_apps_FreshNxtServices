package io.tenseventyseven.fresh.zest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import de.dlyt.yanndroid.oneui.R.attr;
import de.dlyt.yanndroid.oneui.R.color;
import de.dlyt.yanndroid.oneui.R.dimen;
import de.dlyt.yanndroid.oneui.R.drawable;
import de.dlyt.yanndroid.oneui.R.id;
import de.dlyt.yanndroid.oneui.R.layout;
import de.dlyt.yanndroid.oneui.R.menu;
import de.dlyt.yanndroid.oneui.R.string;
import de.dlyt.yanndroid.oneui.R.styleable;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.ProgressBar;
import io.tenseventyseven.fresh.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AboutActivityClass extends LinearLayout {
    private boolean mIsOneUI4;
    public static final int NOT_UPDATEABLE = -1;
    public static final int LOADING = 0;
    public static final int UPDATE_AVAILABLE = 1;
    public static final int NO_UPDATE = 2;
    public static final int NO_CONNECTION = 3;
    private ToolbarLayout toolbarLayout;
    private LinearLayout about_content;
    private TextView app_name;
    private TextView version;
    private TextView status_text;
    private TextView about_optional_text;
    private TextView about_optional_text_2;
    private MaterialButton update_button;
    private MaterialButton retry_button;
    private ProgressBar loading_bar;
    private String optional_text;
    private String optional_text_2;
    private int update_state;

    @SuppressLint("WrongConstant")
    public AboutActivityClass(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mIsOneUI4 = context.getTheme().obtainStyledAttributes(new int[]{attr.isOneUI4}).getBoolean(0, false);
        TypedArray attr = context.getTheme().obtainStyledAttributes(attrs, styleable.AboutPage, 0, 0);

        try {
            this.optional_text = attr.getString(styleable.AboutPage_optional_text);
            this.update_state = attr.getInt(styleable.AboutPage_update_state, 0);
        } finally {
            attr.recycle();
        }

        LayoutInflater var4 = (LayoutInflater)context.getSystemService("layout_inflater");
        var4.inflate(R.layout.zest_activity_about_template, this, true);
        this.toolbarLayout = (ToolbarLayout)this.findViewById(id.toolbar_layout);
        this.about_content = (LinearLayout)this.findViewById(R.id.zest_about_content);
        this.app_name = (TextView)this.findViewById(id.app_name);
        this.version = (TextView)this.findViewById(id.version);
        this.status_text = (TextView)this.findViewById(id.status_text);
        this.about_optional_text = (TextView)this.findViewById(id.about_optional_text);
        this.about_optional_text_2 = (TextView)this.findViewById(R.id.about_optional_text_2);
        this.update_button = (MaterialButton)this.findViewById(id.update_button);
        this.retry_button = (MaterialButton)this.findViewById(id.retry_button);
        this.loading_bar = (ProgressBar)this.findViewById(id.loading_bar);
        this.setOptionalText(this.optional_text);
        this.setUpdateState(this.update_state);
        this.toolbarLayout.findViewById(id.toolbar_layout_app_bar).setBackgroundColor(this.getResources().getColor(color.splash_background));
        this.toolbarLayout.findViewById(id.toolbar_layout_collapsing_toolbar_layout).setBackgroundColor(this.getResources().getColor(color.splash_background));
        this.toolbarLayout.setNavigationButtonIcon(this.getResources().getDrawable(drawable.ic_oui_back, context.getTheme()));
        this.toolbarLayout.setNavigationButtonTooltip(this.getResources().getText(string.sesl_navigate_up));
        this.toolbarLayout.setNavigationButtonOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                AboutActivityClass.this.getActivity().onBackPressed();
            }
        });
        this.toolbarLayout.inflateToolbarMenu(menu.oui_about_page);
        this.toolbarLayout.setOnToolbarMenuItemClickListener((item) -> {
            if (item.getItemId() == id.app_info) {
                try {
                    Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.addFlags(268468224);
                    intent.setData(Uri.parse("package:" + this.getActivity().getApplicationContext().getPackageName()));
                    this.getActivity().startActivity(intent);
                } catch (ActivityNotFoundException var3) {
                    this.getActivity().startActivity(new Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return false;
        });
        if (this.mIsOneUI4) {
            this.app_name.setTypeface(Typeface.create(this.getResources().getString(string.sesl_font_family_regular), 0));
            this.app_name.setTextSize(0, this.getResources().getDimension(dimen.sesl4_about_app_name_text_size));
            this.version.setTextSize(0, this.getResources().getDimension(dimen.sesl4_about_secondary_text_size));
            this.about_optional_text.setTextSize(0, this.getResources().getDimension(dimen.sesl4_about_secondary_text_size));
            this.about_optional_text_2.setTextSize(0, this.getResources().getDimension(dimen.sesl4_about_secondary_text_size));
            this.status_text.setTextSize(0, this.getResources().getDimension(dimen.sesl4_about_secondary_text_size));
        }

        try {
            if (!this.isInEditMode()) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                this.version.setText(context.getString(string.sesl_version) + " " + packageInfo.versionName);
            }
        } catch (NameNotFoundException var8) {
            var8.printStackTrace();
        }

    }

    private Activity getActivity() {
        for(Context context = this.getContext(); context instanceof ContextWrapper; context = ((ContextWrapper)context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
        }

        return null;
    }

    public void setUpdateButtonOnClickListener(OnClickListener listener) {
        this.update_button.setOnClickListener(listener);
    }

    public void setRetryButtonOnClickListener(OnClickListener listener) {
        this.retry_button.setOnClickListener(listener);
    }

    @SuppressLint("WrongConstant")
    public void setUpdateState(int state) {
        switch(state) {
            case -1:
                this.loading_bar.setVisibility(8);
                this.update_button.setVisibility(8);
                this.retry_button.setVisibility(8);
                this.status_text.setVisibility(8);
                break;
            case 0:
                this.loading_bar.setVisibility(0);
                this.update_button.setVisibility(8);
                this.retry_button.setVisibility(8);
                this.status_text.setVisibility(8);
                break;
            case 1:
                this.loading_bar.setVisibility(8);
                this.update_button.setVisibility(0);
                this.retry_button.setVisibility(8);
                this.status_text.setVisibility(0);
                this.status_text.setText(string.new_version_is_available);
                break;
            case 2:
                this.loading_bar.setVisibility(8);
                this.update_button.setVisibility(8);
                this.retry_button.setVisibility(8);
                this.status_text.setVisibility(0);
                this.status_text.setText(string.latest_version_installed);
                break;
            case 3:
                this.loading_bar.setVisibility(8);
                this.update_button.setVisibility(8);
                this.retry_button.setVisibility(0);
                this.status_text.setVisibility(0);
                this.status_text.setText(string.network_connect_is_not_stable);
        }

    }

    @SuppressLint("WrongConstant")
    public void setOptionalText(String text) {
        this.optional_text = text;
        this.about_optional_text.setText(text);
        this.about_optional_text.setVisibility(text != null && !text.isEmpty() ? 0 : 8);
    }

    @SuppressLint("WrongConstant")
    public void setOptionalText2(String text) {
        this.optional_text_2 = text;
        this.about_optional_text_2.setText(text);
        this.about_optional_text_2.setVisibility(text != null && !text.isEmpty() ? 0 : 8);
    }

    public void setToolbarExpandable(boolean expandable) {
        this.toolbarLayout.setExpandable(expandable);
    }

    public void addView(View child, int index, LayoutParams params) {
        if (this.about_content == null) {
            super.addView(child, index, params);
        } else {
            this.about_content.addView(child, index, params);
        }

    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UpdateState {
    }
}
