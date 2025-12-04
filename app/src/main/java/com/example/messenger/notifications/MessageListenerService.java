package com.example.messenger.notifications;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.messenger.R;
import com.example.messenger.message.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MessageListenerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        // создаём канал
        NotificationHelper.createChannel(this);

        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsRef.get().addOnSuccessListener(snapshot -> {

            for (DataSnapshot chat : snapshot.getChildren()) {

                String chatId = chat.getKey();
                if (chatId == null) continue;

                // chatId формата: user1_user2
                if (!chatId.contains(myId)) continue;

                listenForMessages(chatId, myId);
            }
        });
    }

    private void listenForMessages(String chatId, String myId) {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        long currentTime = System.currentTimeMillis();  // Время запуска слушателя

        // Фильтр: только сообщения после currentTime (новые)
        Query query = ref.orderByChild("timestamp").startAfter(currentTime - 1000);  // -1000 на случай задержки часов

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String prev) {

                Message msg = snapshot.getValue(Message.class);
                if (msg == null) return;

                if (msg.getOwnerId() != null && msg.getOwnerId().equals(myId))
                    return;

                NotificationHelper.showMessageNotification(
                        MessageListenerService.this,
                        "Новое сообщение",
                        msg.getText()
                );
            }

            @Override public void onChildChanged(DataSnapshot s, String p) {}
            @Override public void onChildRemoved(DataSnapshot s) {}
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
