package com.example.messenger.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.messenger.R;
import com.example.messenger.message.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MessageListenerService extends Service {

    private static final String TAG = "MessageListenerService";
    private static final String CHANNEL_ID = "messages_channel";
    private static final int FOREGROUND_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");

        // Создаём канал уведомлений
        NotificationHelper.createChannel(this);

        // Запускаем как Foreground Service (для Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_ID, createForegroundNotification());
        }

        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) {
            Log.e(TAG, "User not authenticated");
            stopSelf();
            return;
        }

        Log.d(TAG, "Starting to listen for messages for user: " + myId);
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsRef.get().addOnSuccessListener(snapshot -> {
            Log.d(TAG, "Chats loaded: " + snapshot.getChildrenCount());

            for (DataSnapshot chat : snapshot.getChildren()) {
                String chatId = chat.getKey();
                if (chatId == null) continue;

                String user1 = chat.child("user1").getValue(String.class);
                String user2 = chat.child("user2").getValue(String.class);

                if (myId.equals(user1) || myId.equals(user2)) {
                    String otherUserId = myId.equals(user1) ? user2 : user1;
                    Log.d(TAG, "Setting up listener for chat: " + chatId);
                    listenForMessages(chatId, myId, otherUserId);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load chats: " + e.getMessage());
        });
    }

    private Notification createForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Message Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Messenger")
                .setContentText("Listening for new messages")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void listenForMessages(String chatId, String myId, String otherUserId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        long currentTime = System.currentTimeMillis();

        Query query = ref.orderByChild("timestamp").startAfter(currentTime);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                try {
                    String ownerId = snapshot.child("ownerId").getValue(String.class);
                    String text = snapshot.child("text").getValue(String.class);
                    String fileType = snapshot.child("fileType").getValue(String.class);

                    Log.d(TAG, "New message received in chat: " + chatId);

                    // Игнорируем свои сообщения
                    if (ownerId != null && ownerId.equals(myId)) {
                        Log.d(TAG, "Ignoring own message");
                        return;
                    }

                    // Определяем текст уведомления
                    String notificationText = text;
                    if (fileType != null && !fileType.isEmpty()) {
                        switch (fileType) {
                            case "image":
                                notificationText = "📷 Фото";
                                break;
                            case "video":
                                notificationText = "🎥 Видео";
                                break;
                            case "voice":
                                notificationText = "🎤 Голосовое сообщение";
                                break;
                            case "document":
                                notificationText = "📄 Документ";
                                break;
                        }
                    }

                    if (notificationText == null || notificationText.isEmpty()) {
                        notificationText = "Новое сообщение";
                    }

                    loadUsernameAndNotify(otherUserId, notificationText, chatId);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void loadUsernameAndNotify(String userId, String messageText, String chatId) {
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.getValue(String.class);
                        if (username == null) username = "Unknown User";

                        Log.d(TAG, "Showing notification from: " + username);

                        NotificationHelper.showMessageNotification(
                                MessageListenerService.this,
                                username,
                                messageText,
                                chatId
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load username: " + error.getMessage());

                        NotificationHelper.showMessageNotification(
                                MessageListenerService.this,
                                "New Message",
                                messageText,
                                chatId
                        );
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return START_STICKY; // Сервис будет перезапущен, если убит системой
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
}