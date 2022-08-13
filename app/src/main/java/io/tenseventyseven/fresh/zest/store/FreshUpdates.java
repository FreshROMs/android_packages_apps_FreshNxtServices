package io.tenseventyseven.fresh.zest.store;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class FreshUpdates {

    private static final String FETCH_INSTANCE_NAME = "FreshUpdates";
    private static final String FETCH_API_URL = "https://ota.fresh.tenseventyseven.cf/apps/";

    public static class Update {
        public String name, packageName, summary, changelog, versionName, fileUrl, iconUrl;
        public long versionCode, fileSize;
    }

    public interface ResultListener {
        void onSuccess(Object o);

        void onFailed();
    }

    public interface DownloadListener extends ResultListener {
        void onProgress(int progress, long eta, long speed);
    }

    public static void getStoreList(ResultListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URLConnection connection = new URL(FETCH_API_URL).openConnection();
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) throw new Exception("null");

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line)/*.append("\n")*/;
                if (buffer.length() == 0) throw new Exception("null");

                handler.post(() -> {
                    try {
                        listener.onSuccess(parseJson(buffer.toString()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onFailed();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(listener::onFailed);
            }
        });
    }

    private static ArrayList<Update> parseJson(String response) throws JSONException {
        ArrayList<Update> list = new ArrayList<>();
        JSONArray jsonList = new JSONObject(response).getJSONArray("response");
        for (int i = 0; i < jsonList.length(); i++) {
            Update listItem = new Update();
            JSONObject jsonItem = jsonList.getJSONObject(i);
            listItem.name = jsonItem.optString("name");
            listItem.packageName = jsonItem.optString("packageName");
            listItem.summary = jsonItem.optString("summary");
            listItem.changelog = jsonItem.optString("changelog");
            listItem.name = jsonItem.optString("name");
            listItem.versionName = jsonItem.optString("versionName");
            listItem.fileUrl = jsonItem.optString("file");
            listItem.iconUrl = jsonItem.optString("icon");
            listItem.versionCode = jsonItem.optInt("versionCode");
            listItem.fileSize = jsonItem.optLong("size");
            list.add(listItem);
        }
        return list;
    }

    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) return false;

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    public static boolean isConnectionUnmetered(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) return false;

        return isDeviceOnline(context) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }

    public static void downloadFile(Context context, Update update, DownloadListener listener) {
        File file = new File(context.getFilesDir(), update.packageName + "_" + update.versionName + ".apk");
        if (file.exists()) file.delete();

        final Request request = new Request(update.fileUrl, file.getPath());
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);

        Fetch fetch = getFetch(context);
        fetch.enqueue(request, null, null);
        fetch.addListener(new AbstractFetchListener() {
            @Override
            public void onCompleted(@NonNull Download download) {
                listener.onSuccess(file);
            }

            @Override
            public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
                listener.onFailed();
            }

            @Override
            public void onProgress(@NonNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
                listener.onProgress(download.getProgress(), etaInMilliSeconds, downloadedBytesPerSecond);
            }
        });
    }

    private static Fetch getFetch(Context context) {
        FetchConfiguration.Builder fc = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(1)
                .setAutoRetryMaxAttempts(5)
                .setNamespace(FETCH_INSTANCE_NAME)
                .enableLogging(true)
                .enableAutoStart(true);

        return Fetch.Impl.getInstance(fc.build());
    }

    public static String getFormattedFileSize(long fileSize) {
        if (fileSize <= 0) return "0B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1000));
        return new DecimalFormat("#,##0.#").format(fileSize / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
    }

    public static String getFormattedSpeed(long fileSize) {
        return String.format("%s/s", getFormattedFileSize(fileSize));
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        return context.getPackageManager().isPackageAvailable(packageName);
    }

    public static boolean isPackageSystem(Context context, String packageName) {
        try {
            return (context.getPackageManager().getApplicationInfo(packageName, 0).flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static long getPackageVersionCode(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getPackageVersionName(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "-";
        }
    }

    public static void installPackage(Context context, File packageFile, ResultListener listener) {
        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            packageInstaller.registerSessionCallback(new PackageInstaller.SessionCallback() {
                @Override
                public void onCreated(int sessionId) {
                }

                @Override
                public void onBadgingChanged(int sessionId) {
                }

                @Override
                public void onActiveChanged(int sessionId, boolean active) {
                }

                @Override
                public void onProgressChanged(int sessionId, float progress) {
                }

                @Override
                public void onFinished(int sessionId, boolean success) {
                    if (success) listener.onSuccess(null);
                    else listener.onFailed();
                }
            });
            PackageInstaller.Session session = packageInstaller.openSession(packageInstaller.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)));

            FileInputStream in = new FileInputStream(packageFile);
            OutputStream out = session.openWrite("FreshUpdates", 0, -1);
            byte[] buffer = new byte[1048576];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            in.close();
            out.flush();
            out.close();

            session.commit(PendingIntent.getActivity(context, 0, new Intent(), 0).getIntentSender());
        } catch (IOException e) {
            e.printStackTrace();
            listener.onFailed();
        }

    }

    public static void deletePackage(Context context, String packageName, ResultListener listener) {
        //todo: Option 1 - this works without package installer | requires android.permission.DELETE_PACKAGES | no confirm dialog
        /*try {
            PackageManager pm = context.getPackageManager();
            pm.getClass().getMethod("deletePackage", String.class, IPackageDeleteObserver.class, int.class)
                    .invoke(pm, packageName, new IPackageDeleteObserver() {
                        @Override
                        public void packageDeleted(String packageName, int returnCode) {
                            if (returnCode == PackageManager.DELETE_SUCCEEDED) {
                                listener.onSuccess(null);
                            } else {
                                listener.onFailed();
                            }
                        }

                        @Override
                        public IBinder asBinder() {
                            return null;
                        }
                    }, 0);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }*/

        //todo: Option 2 - using package installer but no callback -> list item won't update
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        context.startActivity(intent);
    }

    public static boolean canLaunchApp(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName) != null;
    }

    public static void launchApp(Context context, String packageName) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) return;
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }

}
