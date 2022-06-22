package io.tenseventyseven.fresh.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

public class Notifications {
    public static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, Class<?> cls) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);

        Intent resultIntent = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(cls);
        stackBuilder.addNextIntent(resultIntent);

        builder.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM);

        return builder;
    }

    public static void cancelOngoingNotification(Context context, int notificationId) {
        getNotificationManager(context).cancel(notificationId);
    }

    public static void setupNotificationGroup(Context context, String groupId, int groupRes) {
        NotificationManager notificationManager = getNotificationManager(context);

        // Setup channel group
        CharSequence groupName = context.getString(groupRes);
        NotificationChannelGroup notificationGroup = new NotificationChannelGroup(groupId, groupName);

        // Commit
        notificationManager.createNotificationChannelGroup(notificationGroup);
    }

    public static void setupNotificationChannel(Context context, String groupId, String channelId, int channelName, int channelDesc, int importance) {
        NotificationManager notificationManager = getNotificationManager(context);

        // Set channel name, group, and description
        String name = context.getString(channelName);
        String description = context.getString(channelDesc);

        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setGroup(groupId);
        channel.setDescription(description);

        // Commit
        notificationManager.createNotificationChannel(channel);
    }
}
