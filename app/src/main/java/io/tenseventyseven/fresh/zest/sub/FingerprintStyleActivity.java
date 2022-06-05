package io.tenseventyseven.fresh.zest.sub;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
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
import com.samsung.android.biometrics.ISemBiometricSysUiService;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.widget.RoundFrameLayout;
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
    @BindView(R.id.preview_frame)
    RoundFrameLayout preview_frame;
    @BindView(R.id.preview_homescreen)
    FrameLayout preview_homescreen;
    @BindView(R.id.preview_lock_background)
    ImageView preview_lock_background;
    @BindView(R.id.preview_lock_foreground)
    ImageView preview_lock_foreground;
    @BindView(R.id.preview_home_background)
    ImageView preview_home_background;
    @BindView(R.id.preview_home_foreground)
    ImageView preview_home_foreground;
    @BindView(R.id.preview_animation)
    LottieAnimationView preview_animation;
    @BindView(R.id.preview_fp_icon)
    ImageView preview_fp_icon;
    @BindView(R.id.preview_listView)
    RecyclerView preview_listView;

    private File[] animFiles;
    private int selectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_fingerprint_style_settings);
        ButterKnife.bind(this);

        animFiles = new File(animDirPath).listFiles((file, s) -> s.endsWith(".json"));
        if (animFiles == null) animFiles = new File[0];

        selectedPosition = 0; //todo: get current

        toolbar.setExpanded(false, false);
        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
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
        preview_lock_foreground.setImageBitmap(getLockScreenPreview(true));
        preview_home_background.setImageBitmap(getWallpaper(true));
        preview_home_foreground.setImageBitmap(getHomeScreenPreview(true));

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
                handler.postDelayed(() -> preview_homescreen.setVisibility(View.GONE), 600);
                handler.postDelayed(() -> preview_animation.playAnimation(), 1200);
            }
        });

        /*Bitmap lockBG = getWallpaper(false);
        int bgLength = (int) (size.x * (double) (lockBG.getHeight() / size.y));
        Bitmap listItemBG = Bitmap.createBitmap(lockBG, (lockBG.getWidth() - bgLength) / 2, lockBG.getHeight() - bgLength, bgLength, bgLength);*/

        preview_listView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        preview_listView.setAdapter(new PreviewAdapter());

        if (animFiles.length > selectedPosition) {
            try {
                preview_animation.setAnimation(new FileInputStream(animFiles[selectedPosition]), null);
                preview_animation.playAnimation();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void onTapCancel(View v) {
        onBackPressed();
    }

    public void onTapDone(View v) { //todo: apply fingerprint animation and save current position
        if (animFiles.length <= selectedPosition) {
            Toast.makeText(this, R.string.zest_apply_fp_anim_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkSelfPermission("android.permission.MANAGE_BIOMETRIC") == PackageManager.PERMISSION_GRANTED) {
            //doesn't work cuz 'android.permission.MANAGE_BIOMETRIC' not granted
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.samsung.android.biometrics.app.setting", "com.samsung.android.biometrics.app.setting.BiometricsUIService"));
            bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    try {
                        FileDescriptor fd = new FileInputStream(animFiles[selectedPosition]).getFD();
                        ((ISemBiometricSysUiService) service).setBiometricTheme(10000, "animation", null, fd);
                    } catch (RemoteException | IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            }, Context.BIND_AUTO_CREATE);
        } else {
            //use symlink to /sdcard/Animations/current/user_fingerprint_touch_effect.json

            try {
                File outputFile = new File("/sdcard/Animations/current/user_fingerprint_touch_effect.json");
                outputFile.getParentFile().mkdirs();
                FileInputStream inputStream = new FileInputStream(animFiles[selectedPosition]);
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.zest_apply_fp_anim_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        onBackPressed();
    }

    private Bitmap getWallpaper(boolean homeScreen) {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return null;

        ParcelFileDescriptor wallpaperFile = WallpaperManager.getInstance(this).getWallpaperFile(homeScreen ? WallpaperManager.FLAG_SYSTEM : WallpaperManager.FLAG_LOCK);
        if (wallpaperFile == null) return null;
        return BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
    }

    private Bitmap getHomeScreenPreview(boolean portrait) {
        if (checkSelfPermission("com.android.homescreen.home.permission.preview_image") != PackageManager.PERMISSION_GRANTED)
            return null;

        try {
            String uri = "content://com.android.homescreen.home.WallpaperPreview/" + (portrait ? "portrait" : "landscape");
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), Uri.parse(uri)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getLockScreenPreview(boolean portrait) {
        if (checkSelfPermission("com.samsung.systemui.permission.KEYGUARD_IMAGE") != PackageManager.PERMISSION_GRANTED)
            return null;

        try {
            String uri = "content://com.android.systemui.keyguard.image/" + (portrait ? "portrait" : "landscape") + "?white_theme=off";
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
            return new ViewHolder(getLayoutInflater().inflate(R.layout.zest_fp_preview_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                holder.list_item_lottie.setAnimation(new FileInputStream(animFiles[position]), null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            holder.list_item_lottie.setSelected(selectedPosition == position);
            holder.list_item_lottie.setOnClickListener(v -> {
                notifyItemChanged(selectedPosition);
                preview_animation.cancelAnimation();
                try {
                    preview_animation.setAnimation(new FileInputStream(animFiles[selectedPosition = holder.getAdapterPosition()]), null);
                    preview_animation.playAnimation();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return animFiles.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private LottieAnimationView list_item_lottie;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                list_item_lottie = itemView.findViewById(R.id.list_item_lottie);
            }
        }
    }

}