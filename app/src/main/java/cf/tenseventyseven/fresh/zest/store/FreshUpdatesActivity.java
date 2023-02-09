package cf.tenseventyseven.fresh.zest.store;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.dialog.ProgressDialog;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.sesl.recyclerview.LinearLayoutManager;
import de.dlyt.yanndroid.oneui.view.RecyclerView;
import de.dlyt.yanndroid.oneui.widget.ProgressBar;
import cf.tenseventyseven.fresh.R;

public class FreshUpdatesActivity extends AppCompatActivity {

    @BindView(R.id.zest_fresh_updates_toolbar)
    ToolbarLayout toolbar;

    @BindView(R.id.zest_fresh_updates_list)
    RecyclerView listView;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_fresh_updates);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_CIRCLE);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        FreshUpdates.getStoreList(new FreshUpdates.ResultListener() {
            @Override
            public void onSuccess(Object o) {
                progressDialog.dismiss();
                ArrayList<FreshUpdates.Update> list = (ArrayList<FreshUpdates.Update>) o;
                initListView(list);
            }

            @Override
            public void onFailed() {
                progressDialog.dismiss();
            }
        });

    }

    private void initListView(ArrayList<FreshUpdates.Update> list) {
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(new UpdatesAdapter(this, list));
        listView.addItemDecoration(new ItemDecoration(this));
        listView.setItemAnimator(null);
        listView.seslSetFillBottomEnabled(true);
        listView.seslSetLastRoundedCorner(true);
        listView.seslSetFastScrollerEnabled(true);
        listView.seslSetGoToTopEnabled(true);
        listView.seslSetSmoothScrollEnabled(true);
    }

    public class UpdatesAdapter extends RecyclerView.Adapter<UpdatesAdapter.ViewHolder> {
        private Context mContext;
        private ArrayList<FreshUpdates.Update> mList;

        UpdatesAdapter(Context context, ArrayList<FreshUpdates.Update> list) {
            this.mContext = context;
            this.mList = list;
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        @NonNull
        @Override
        public UpdatesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UpdatesAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.zest_fresh_updates_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(UpdatesAdapter.ViewHolder holder, final int position) {
            final FreshUpdates.Update update = mList.get(position);

            holder.uIcon.setClipToOutline(true);
            Picasso.get().load(update.iconUrl).noPlaceholder().into(holder.uIcon, new Callback() {
                @Override
                public void onSuccess() {
                    holder.uLoad.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    holder.uLoad.setVisibility(View.GONE);
                    holder.uIcon.setImageResource(R.drawable.zest_fresh_updates_no_icon);
                }
            });

            holder.uName.setText(update.name);
            holder.uPackage.setText(update.packageName);
            holder.uVersion.setText(getText(R.string.fresh_ota_changelog_detail_version) + " " + FreshUpdates.getPackageVersionName(mContext, update.packageName));
            holder.uSummary.setText(update.summary);

            if (update.versionCode > FreshUpdates.getPackageVersionCode(mContext, update.packageName)) {
                holder.aDownload.setImageResource(FreshUpdates.isPackageInstalled(mContext, update.packageName) ? R.drawable.ic_oui_refresh : R.drawable.ic_oui_download);
                holder.aDownload.setVisibility(View.VISIBLE);
                holder.aDownload.setOnClickListener(v -> {
                    holder.aContainer.setVisibility(View.GONE);
                    holder.pContainer.setVisibility(View.VISIBLE);
                    holder.pProgress.setIndeterminate(false);
                    holder.pProgress.setProgress(0);
                    holder.pETA.setText(null);
                    holder.pSpeed.setText(null);

                    FreshUpdates.downloadFile(mContext, update, new FreshUpdates.DownloadListener() {
                        @Override
                        public void onProgress(int progress, long eta, long speed) {
                            holder.pProgress.setProgress(progress);
                            holder.pETA.setText(new SimpleDateFormat("hh:mm:ss").format(new Date(eta)));
                            holder.pSpeed.setText(FreshUpdates.getFormattedSpeed(speed));
                        }

                        @Override
                        public void onFailed() {
                            holder.pContainer.setVisibility(View.GONE);
                            holder.aContainer.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onSuccess(Object o) {
                            holder.pETA.setText(null);
                            holder.pSpeed.setText(null);
                            holder.pProgress.setIndeterminate(true);

                            FreshUpdates.installPackage(mContext, (File) o, new FreshUpdates.ResultListener() {
                                @Override
                                public void onSuccess(Object o) {
                                    notifyItemChanged(position);
                                    holder.pContainer.setVisibility(View.GONE);
                                    holder.aContainer.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onFailed() {
                                    holder.pContainer.setVisibility(View.GONE);
                                    holder.aContainer.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                });

                holder.cContainer.setVisibility(View.VISIBLE);
                holder.cTitle.setText(update.versionName + " - " + getText(R.string.fresh_ota_changelog_header_title));
                holder.cSummary.setText(update.changelog);
                holder.cSize.setText(getText(R.string.fresh_ota_changelog_detail_size) + " " + FreshUpdates.getFormattedFileSize(update.fileSize));
            } else {
                holder.aDownload.setVisibility(View.GONE);
                holder.aDownload.setOnClickListener(null);
                holder.cContainer.setVisibility(View.GONE);
            }

            holder.aLaunch.setVisibility(FreshUpdates.canLaunchApp(mContext, update.packageName) ? View.VISIBLE : View.GONE);
            holder.aLaunch.setOnClickListener(v -> FreshUpdates.launchApp(mContext, update.packageName));

            holder.aDelete.setVisibility((FreshUpdates.isPackageInstalled(mContext, update.packageName) && !FreshUpdates.isPackageSystem(mContext, update.packageName)) ? View.VISIBLE : View.GONE);
            holder.aDelete.setOnClickListener(v -> {
                holder.aContainer.setVisibility(View.GONE);
                FreshUpdates.deletePackage(mContext, update.packageName, new FreshUpdates.ResultListener() {
                    @Override
                    public void onSuccess(Object o) {
                        holder.aContainer.setVisibility(View.VISIBLE);
                        notifyItemChanged(position);
                    }

                    @Override
                    public void onFailed() {
                        holder.aContainer.setVisibility(View.VISIBLE);
                    }
                });
            });

            //todo get fetch downloader + progress
            holder.aContainer.setVisibility(View.VISIBLE);

        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView uIcon;
            ProgressBar uLoad;
            TextView uName, uPackage, uVersion, uSummary;
            LinearLayout aContainer;
            ImageView aLaunch, aDownload, aDelete;
            LinearLayout cContainer;
            TextView cTitle, cSummary, cSize;
            LinearLayout pContainer;
            ProgressBar pProgress;
            TextView pSpeed, pETA;

            ViewHolder(View itemView) {
                super(itemView);
                uIcon = itemView.findViewById(R.id.fresh_update_icon);
                uLoad = itemView.findViewById(R.id.fresh_update_loading);
                uName = itemView.findViewById(R.id.fresh_update_title);
                uPackage = itemView.findViewById(R.id.fresh_update_package);
                uVersion = itemView.findViewById(R.id.fresh_update_version);
                uSummary = itemView.findViewById(R.id.fresh_update_summary);
                aContainer = itemView.findViewById(R.id.fresh_update_actions);
                aLaunch = itemView.findViewById(R.id.fresh_update_launch);
                aDownload = itemView.findViewById(R.id.fresh_update_download);
                aDelete = itemView.findViewById(R.id.fresh_update_delete);
                cContainer = itemView.findViewById(R.id.fresh_update_changelog);
                cTitle = itemView.findViewById(R.id.fresh_update_changelog_title);
                cSummary = itemView.findViewById(R.id.fresh_update_changelog_summary);
                cSize = itemView.findViewById(R.id.fresh_update_changelog_size);
                pContainer = itemView.findViewById(R.id.fresh_update_progress);
                pProgress = itemView.findViewById(R.id.fresh_update_progress_bar);
                pSpeed = itemView.findViewById(R.id.fresh_update_progress_speed);
                pETA = itemView.findViewById(R.id.fresh_update_progress_eta);
            }
        }
    }

    private class ItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable mDivider;

        public ItemDecoration(@NonNull Context context) {
            mDivider = context.getDrawable(R.drawable.sesl_list_divider);
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);

            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                final int top = child.getBottom()
                        + ((ViewGroup.MarginLayoutParams) child.getLayoutParams()).bottomMargin;
                final int bottom = mDivider.getIntrinsicHeight() + top;

                mDivider.setBounds(parent.getLeft(), top, parent.getRight(), bottom);
                mDivider.draw(c);
            }
        }
    }

}
