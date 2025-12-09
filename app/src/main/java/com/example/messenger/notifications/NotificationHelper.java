package com.example.messenger.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.messenger.ChatActivity;
import com.example.messenger.R;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "messages_channel";

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Сообщения",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Уведомления о новых сообщениях");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    public static void showMessageNotification(Context ctx, String title, String text, String chatId) {
        try {
            Log.d(TAG, "Showing notification - Title: " + title + ", Text: " + text);

            // Создаём Intent для открытия ChatActivity
            Intent chatIntent = new Intent(ctx, ChatActivity.class);
            chatIntent.putExtra("chatId", chatId);
            chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // PendingIntent с правильными флагами
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    ctx,
                    chatId.hashCode(), // Уникальный ID для каждого чата
                    chatIntent,
                    flags
            );

            // Создаём уведомление
            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                // Используем hashCode chatId как уникальный ID уведомления
                nm.notify(chatId.hashCode(), builder.build());
                Log.d(TAG, "Notification shown successfully");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}