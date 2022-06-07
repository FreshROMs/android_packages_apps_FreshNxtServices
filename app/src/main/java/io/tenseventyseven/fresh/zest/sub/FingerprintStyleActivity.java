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
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.airbnb.lottie.LottieAnimationView;
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.view.Toast;
import io.tenseventyseven.fresh.ExperienceUtils;
import io.tenseventyseven.fresh.R;

public class FingerprintStyleActivity extends AppCompatActivity {

    private static final int sensorPositionY = 2065;

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
    CardView mPreviewFrame;
    @BindView(R.id.zest_fod_animation_style_preview)
    FrameLayout mPreview;

    @BindView(R.id.zest_fod_animation_style_preview_lock_bg)
    ImageView mPreviewLockBG;
    @BindView(R.id.zest_fod_animation_style_preview_lock_fg)
    ImageView mPreviewLockFG;

    @BindView(R.id.zest_fod_animation_style_preview_home_bg)
    ImageView mPreviewHomeBG;
    @BindView(R.id.zest_fod_animation_style_preview_home_fg)
    ImageView mPreviewHomeFG;

    @BindView(R.id.zest_fod_animation_style_preview_anim)
    LottieAnimationView mPreviewLottieAnim;
    @BindView(R.id.zest_fod_animation_style_preview_fod_icon)
    ImageView mPreviewFpIcon;

    @BindView(R.id.zest_fod_animation_style_listview)
    RecyclerView mPickerRecylerView;

    String[] mFodAnimationIdentifiers;
    private final static String mFodAnimationPackage = "io.tenseventyseven.fresh.udfps.res";

    Context mContext;
    private int mSelectedAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_fod_animation_style_settings);
        ButterKnife.bind(this);
        mContext = this;

        if (!getAnimations())
            finish();

        mSelectedAnim = Settings.System.getInt(getContentResolver(), "zest_fod_animation_selected", 0);

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonVisible(false);
        toolbar.setBackgroundResource(R.drawable.sesl4_action_bar_background);
        setSupportActionBar(toolbar.getToolbar());

        Point size = new Point();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
        double screenRatio = (double) size.x / (double) size.y;

        mPreviewFrame.post(() -> {
            mPreviewFrame.setLayoutParams(new LinearLayout.LayoutParams((int) (mPreviewFrame.getHeight() * screenRatio), ViewGroup.LayoutParams.MATCH_PARENT, 1));
            double previewScale = (double) mPreviewFrame.getHeight() / (double) size.y;
            mPreviewLottieAnim.post(() -> mPreviewLottieAnim.setTranslationY((float) ((sensorPositionY * previewScale) - (mPreviewLottieAnim.getHeight() / 2))));
            mPreviewFpIcon.post(() -> mPreviewFpIcon.setTranslationY((float) ((sensorPositionY * previewScale) - (mPreviewFpIcon.getHeight() / 2))));
        });

        mPreviewLockBG.setImageBitmap(getWallpaper(false));
        mPreviewLockFG.setImageBitmap(getPreview(false));

        mPreviewHomeBG.setImageBitmap(getWallpaper(true));
        mPreviewHomeFG.setImageBitmap(getPreview(true));

        Handler handler = new Handler();
        mPreviewLottieAnim.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                handler.removeCallbacksAndMessages(null);
                mPreviewLottieAnim.setAlpha(1F);
                mPreview.setVisibility(View.GONE);
                handler.postDelayed(() -> {
                    mPreview.setVisibility(View.VISIBLE);
                }, (long) (mPreviewLottieAnim.getDuration() * 0.7));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mPreviewLottieAnim.setAlpha(0F);
                handler.postDelayed(() -> mPreview.setVisibility(View.GONE), 1000);
                handler.postDelayed(() -> mPreviewLottieAnim.playAnimation(), 2000);
            }
        });

        /*Bitmap lockBG = getWallpaper(false);
        int bgLength = (int) (size.x * (double) (lockBG.getHeight() / size.y));
        Bitmap listItemBG = Bitmap.createBitmap(lockBG, (lockBG.getWidth() - bgLength) / 2, lockBG.getHeight() - bgLength, bgLength, bgLength);*/

        mPickerRecylerView.setAdapter(new PreviewAdapter());
        mPickerRecylerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        SnapHelper snapHelper = new GravitySnapHelper(Gravity.START);
        snapHelper.attachToRecyclerView(mPickerRecylerView);

        mPickerRecylerView.scrollToPosition(mSelectedAnim);

        if (mFodAnimationIdentifiers.length > mSelectedAnim) {
            mPreviewLottieAnim.setAnimation(getLottieJson(mContext, mFodAnimationIdentifiers[mSelectedAnim]), null);
            mPreviewLottieAnim.playAnimation();
        }
    }

    public void onTapCancel(View v) {
        onBackPressed();
    }

    public void onTapDone(View v) {
        int oldInt = Settings.System.getInt(getContentResolver(), "zest_fod_animation_selected", 0);
        String oldString = Settings.System.getString(getContentResolver(), "zest_fod_animation_id");

        Settings.System.putInt(getContentResolver(), "zest_fod_animation_selected", mSelectedAnim);
        Settings.System.putString(getContentResolver(), "zest_fod_animation_id", mFodAnimationIdentifiers[mSelectedAnim]);

        new Thread(() -> {
            File folder = ExperienceUtils.getFreshDir();

            if (!folder.exists())
                folder.mkdir();

            File file = new File(folder, "user_fingerprint_touch_effect.tmp");
            File animJson = new File(folder, "user_fingerprint_touch_effect.json");
            InputStream input = getLottieJson(mContext, mFodAnimationIdentifiers[mSelectedAnim]);

            try (OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                output.flush();
            } catch (IOException e) {
                file.delete();
                e.printStackTrace();
            }

            if (!file.exists()) {
                Toast.makeText(mContext, R.string.zest_plus_fod_animation_style_toast_failed, Toast.LENGTH_SHORT).show();
                Settings.System.putInt(getContentResolver(), "zest_fod_animation_selected", oldInt);
                Settings.System.putString(getContentResolver(), "zest_fod_animation_id", oldString);
                return;
            }

            if (animJson.exists())
                animJson.delete();

            file.renameTo(animJson);
        }).start();

        onBackPressed();
    }

    private boolean getAnimations() {
        Resources fodRes;

        try {
            PackageManager pm = getApplicationContext().getPackageManager();
            fodRes = pm.getResourcesForApplication(mFodAnimationPackage);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        mFodAnimationIdentifiers = fodRes.getStringArray(fodRes.getIdentifier("udfps_animation_identifiers",
                "array", mFodAnimationPackage));

        return true;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private Drawable getPreviewDrawable(Context context, String fodIdentifier) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mFodAnimationPackage);
            return res.getDrawable(res.getIdentifier("zest_fod_animation_" + fodIdentifier, "drawable", mFodAnimationPackage));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static String getAnimString(Context context, String fodIdentifier) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mFodAnimationPackage);
            return res.getString(res.getIdentifier("zest_fod_animation_" + fodIdentifier, "string", mFodAnimationPackage));
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private InputStream getLottieJson(Context context, String fodIdentifier) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mFodAnimationPackage);
            return res.openRawResource(res.getIdentifier("zest_fod_animation_" + fodIdentifier, "raw", mFodAnimationPackage));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap getWallpaper(boolean isHomeScreen) {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return null;

        ParcelFileDescriptor wallpaperFile = WallpaperManager.getInstance(this).getWallpaperFile(isHomeScreen ? WallpaperManager.FLAG_SYSTEM : WallpaperManager.FLAG_LOCK);
        if (wallpaperFile == null) return null;
        return BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
    }

    private Bitmap getPreview(boolean isHomeScreen) {
        if (checkSelfPermission(isHomeScreen ? "com.android.homescreen.home.permission.preview_image" : "com.samsung.systemui.permission.KEYGUARD_IMAGE") != PackageManager.PERMISSION_GRANTED)
            return null;

        try {
            String uri = isHomeScreen ? "content://com.android.homescreen.home.WallpaperPreview/portrait" : "content://com.android.systemui.keyguard.image/portrait?white_theme=off";
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
            holder.itemImgView.setImageDrawable(getPreviewDrawable(mContext, mFodAnimationIdentifiers[position]));
            holder.itemImgView.setSelected(mSelectedAnim == position);
            holder.itemImgView.setOnClickListener(v -> {
                notifyItemChanged(mSelectedAnim);
                mPreviewLottieAnim.cancelAnimation();
                mPreviewLottieAnim.setAnimation(getLottieJson(mContext, mFodAnimationIdentifiers[mSelectedAnim = holder.getAdapterPosition()]), null);
                mPreviewLottieAnim.playAnimation();
                notifyItemChanged(mSelectedAnim);
            });
        }

        @Override
        public int getItemCount() {
            return mFodAnimationIdentifiers.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView itemImgView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemImgView = itemView.findViewById(R.id.zest_fod_animation_list_image);
            }
        }
    }

}