package io.tenseventyseven.fresh.zest.sub;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.RoundFrameLayout;
import de.dlyt.yanndroid.oneui.widget.Switch;
import io.tenseventyseven.fresh.R;

public class FingerprintStyleActivity extends AppCompatActivity {

    private static final int sensorPositionY = 2065;
    private static final String animDirPath = "/sdcard/Animations";

    /**
     * add to /etc/permissions :
     * <permission name="com.android.homescreen.home.permission.preview_image"/>
     * <permission name="com.samsung.systemui.permission.KEYGUARD_IMAGE"/>
     * <permission name="android.permission.WRITE_EXTERNAL_STORAGE"/>
     * <permission name="android.permission.READ_EXTERNAL_STORAGE"/>
     * <permission name="android.permission.MANAGE_BIOMETRIC"/>
     */

    @BindView(R.id.zest_fingerprint_style_toolbar)
    ToolbarLayout toolbar;
    @BindView(R.id.zest_fod_animation_style_preview_frame)
    RoundFrameLayout preview_frame;
    @BindView(R.id.zest_fod_animation_style_preview_bg_homescreen)
    FrameLayout preview_homescreen;
    @BindView(R.id.zest_fod_animation_style_preview_bg_lockscreen)
    ImageView preview_lock_background;
    @BindView(R.id.zest_fod_animation_style_preview_fg_lockscreen)
    ImageView preview_lock_foreground;
    @BindView(R.id.zest_fod_animation_style_preview_bg)
    ImageView preview_home_background;
    @BindView(R.id.zest_fod_animation_style_preview_fg)
    ImageView preview_home_foreground;
    @BindView(R.id.zest_fod_animation_style_preview)
    LottieAnimationView preview_animation;
    @BindView(R.id.zest_fod_animation_style_preview_fod_icon)
    ImageView preview_fp_icon;
    @BindView(R.id.zest_fod_animation_style_listview)
    RecyclerView preview_listView;

    String[] mFodAnimationIds;
    String[] mFodAnimationNames;
    private final String mFodAnimationPackage = "io.tenseventyseven.fresh.udfps.res";
    private File mAnimationFile;
    private final String mAnimationFileName = "user_fingerprint_touch_effect.json";

    Context mContext;
    private int selectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_fod_animation_style_settings);
        ButterKnife.bind(this);
        mContext = this;

        getAnimations();
        mAnimationFile = new File(getFilesDir(), mAnimationFileName);

        selectedPosition = Settings.System.getInt(getContentResolver(), "zest_fod_animation_selected", 0);

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonVisible(false);
        toolbar.setTitle("");
        toolbar.setBackgroundResource(R.drawable.sesl4_action_bar_background);
        setSupportActionBar(toolbar.getToolbar());

        Point size = new Point();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
        double screenRatio = (double) size.x / (double) size.y;

        preview_frame.post(() -> {
            preview_frame.setLayoutParams(new LinearLayout.LayoutParams((int) (preview_frame.getHeight() * screenRatio), ViewGroup.LayoutParams.MATCH_PARENT, 1));
            double previewScale = (double) preview_frame.getHeight() / (double) size.y;
            preview_animation.post(() -> preview_animation.setTranslationY((float) ((sensorPositionY * previewScale) - (preview_animation.getHeight() / 2))));
            preview_fp_icon.post(() -> preview_fp_icon.setTranslationY((float) ((sensorPositionY * previewScale) - (preview_fp_icon.getHeight() / 2))));
        });

        preview_lock_background.setImageBitmap(getWallpaper(false));
        preview_lock_foreground.setImageBitmap(getLockScreenPreview());
        preview_home_background.setImageBitmap(getWallpaper(true));
        preview_home_foreground.setImageBitmap(getHomeScreenPreview());

        Handler handler = new Handler();
        preview_animation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                handler.removeCallbacksAndMessages(null);
                preview_animation.setAlpha(1F);
                preview_homescreen.setVisibility(View.GONE);
                handler.postDelayed(() -> {
                    preview_homescreen.setVisibility(View.VISIBLE);
                }, (long) (preview_animation.getDuration() * 0.7));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                preview_animation.setAlpha(0F);
                handler.postDelayed(() -> preview_homescreen.setVisibility(View.GONE), 1000);
                handler.postDelayed(() -> preview_animation.playAnimation(), 2000);
            }
        });

        /*Bitmap lockBG = getWallpaper(false);
        int bgLength = (int) (size.x * (double) (lockBG.getHeight() / size.y));
        Bitmap listItemBG = Bitmap.createBitmap(lockBG, (lockBG.getWidth() - bgLength) / 2, lockBG.getHeight() - bgLength, bgLength, bgLength);*/

        preview_listView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        preview_listView.setAdapter(new PreviewAdapter());
        preview_listView.scrollToPosition(selectedPosition);

        if (mFodAnimationIds.length > selectedPosition) {
            preview_animation.setAnimation(getLottieJson(mContext, mFodAnimationIds[selectedPosition]), null);
            preview_animation.playAnimation();
        }
    }

    public void onTapCancel(View v) {
        onBackPressed();
    }

    public void onTapDone(View v) {
        Settings.System.putInt(getContentResolver(), "zest_fod_animation_selected", selectedPosition);
        Settings.System.putString(getContentResolver(), "zest_fod_animation_name", mFodAnimationNames[selectedPosition]);
        InputStream input = getLottieJson(mContext, mFodAnimationIds[selectedPosition]);

        File folder = new File(getAnimDir());
        if (!folder.exists())
            folder.mkdir();

        File file = new File(getAnimDir() + mAnimationFileName);
        try (OutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        onBackPressed();
    }

    public static String getAnimDir() {
        return Environment.getExternalStorageDirectory().getPath() + "/.fresh/";
    }

    private void getAnimations() {
        Resources fodRes;

        try {
            PackageManager pm = getApplicationContext().getPackageManager();
            fodRes = pm.getResourcesForApplication(mFodAnimationPackage);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        mFodAnimationIds = fodRes.getStringArray(fodRes.getIdentifier("udfps_animation_identifiers",
                        "array", mFodAnimationPackage));
        mFodAnimationNames = fodRes.getStringArray(fodRes.getIdentifier("udfps_animation_titles",
                "array", mFodAnimationPackage));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public Drawable getPreviewDrawable(Context context, String fodIdentifier) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mFodAnimationPackage);
            return res.getDrawable(res.getIdentifier("zest_fod_animation_" + fodIdentifier, "drawable", mFodAnimationPackage));
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public InputStream getLottieJson(Context context, String fodIdentifier) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mFodAnimationPackage);
            return res.openRawResource(res.getIdentifier("zest_fod_animation_" + fodIdentifier, "raw", mFodAnimationPackage));
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap getWallpaper(boolean homeScreen) {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return null;

        ParcelFileDescriptor wallpaperFile = WallpaperManager.getInstance(this).getWallpaperFile(homeScreen ? WallpaperManager.FLAG_SYSTEM : WallpaperManager.FLAG_LOCK);
        if (wallpaperFile == null) return null;
        return BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
    }

    private Bitmap getHomeScreenPreview() {
        if (checkSelfPermission("com.android.homescreen.home.permission.preview_image") != PackageManager.PERMISSION_GRANTED)
            return null;

        try {
            String uri = "content://com.android.homescreen.home.WallpaperPreview/portrait";
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), Uri.parse(uri)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getLockScreenPreview() {
        if (checkSelfPermission("com.samsung.systemui.permission.KEYGUARD_IMAGE") != PackageManager.PERMISSION_GRANTED)
            return null;

        try {
            String uri = "content://com.android.systemui.keyguard.image/portrait?white_theme=off";
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), Uri.parse(uri)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.zest_fod_animation_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.list_item_image.setImageDrawable(getPreviewDrawable(mContext, mFodAnimationIds[position]));
            holder.list_item_image.setSelected(selectedPosition == position);
            holder.list_item_image.setOnClickListener(v -> {
                notifyItemChanged(selectedPosition);
                preview_animation.cancelAnimation();
                preview_animation.setAnimation(getLottieJson(mContext, mFodAnimationIds[selectedPosition = holder.getAdapterPosition()]), null);
                preview_animation.playAnimation();
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return mFodAnimationIds.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView list_item_image;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                list_item_image = itemView.findViewById(R.id.zest_fod_animation_list_image);
            }
        }
    }

}