package com.example.messenger.services;

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
import com.example.messenger.config.AppConfig;
import com.example.messenger.notifications.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MessageListenerService extends Service {

    private static final String TAG = "MessageListenerService";
    private static final int FOREGROUND_ID = AppConfig.Notifications.FOREGROUND_SERVICE_ID;
    private static final String FOREGROUND_CHANNEL_ID = AppConfig.Notifications.FOREGROUND_CHANNEL_ID;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_ID, createForegroundNotification());
        }

        NotificationHelper.createChannel(this);

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
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to load chats: " + e.getMessage()));
    }

    private Notification createForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Service channel",
                    NotificationManager.IMPORTANCE_MIN
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Messenger")
                .setContentText("Listening for new messages")
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build();
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
                    String contactUserId = snapshot.child("contactUserId").getValue(String.class);

                    Log.d(TAG, "New message received in chat: " + chatId);

                    if (ownerId != null && ownerId.equals(myId)) {
                        Log.d(TAG, "Ignoring own message");
                        return;
                    }

                    checkNotificationStatusAndNotify(chatId, myId, otherUserId, text, fileType, contactUserId);

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

    private void checkNotificationStatusAndNotify(String chatId, String myId, String otherUserId,
                                                  String text, String fileType, String contactUserId) {
        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("mutedBy")
                .child(myId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isMuted = snapshot.getValue(Boolean.class);

                        if (isMuted != null && isMuted) {
                            Log.d(TAG, "Notifications muted for chat: " + chatId);
                            return;
                        }

                        String notificationText = text;

                        if (contactUserId != null && !contactUserId.isEmpty()) {
                            notificationText = "👤 Contact";
                        } else if (fileType != null && !fileType.isEmpty()) {
                            switch (fileType) {
                                case "image":
                                    notificationText = "📷 Photo";
                                    break;
                                case "video":
                                    notificationText = "🎥 Video";
                                    break;
                                case "voice":
                                    notificationText = "🎤 Voice";
                                    break;
                                case "document":
                                    notificationText = "📄 Document";
                                    break;
                            }
                        }

                        if (notificationText == null || notificationText.isEmpty()) {
                            notificationText = "New message";
                        }

                        loadUsernameAndNotify(otherUserId, notificationText, chatId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to check mute status: " + error.getMessage());
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
        return START_STICKY;
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