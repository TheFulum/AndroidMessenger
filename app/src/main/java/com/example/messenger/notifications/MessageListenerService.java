package com.example.messenger.notifications;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.messenger.message.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MessageListenerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        // Создаём канал для уведомлений сообщений
        NotificationHelper.createChannel(this);

        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot chat : snapshot.getChildren()) {
                String chatId = chat.getKey();
                if (chatId == null) continue;

                // Проверяем, участвует ли текущий пользователь в чате
                String user1 = chat.child("user1").getValue(String.class);
                String user2 = chat.child("user2").getValue(String.class);

                if (myId.equals(user1) || myId.equals(user2)) {
                    String otherUserId = myId.equals(user1) ? user2 : user1;
                    listenForMessages(chatId, myId, otherUserId);
                }
            }
        });
    }

    private void listenForMessages(String chatId, String myId, String otherUserId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        long currentTime = System.currentTimeMillis();

        // Фильтр: только сообщения после currentTime (новые)
        Query query = ref.orderByChild("timestamp").startAfter(currentTime - 1000);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                Message msg = snapshot.getValue(Message.class);
                if (msg == null) return;

                // Игнорируем свои сообщения
                if (msg.getOwnerId() != null && msg.getOwnerId().equals(myId))
                    return;

                // Загружаем имя отправителя и показываем уведомление (добавили chatId)
                loadUsernameAndNotify(otherUserId, msg.getText(), chatId);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Добавили chatId в метод
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

                        // Показываем уведомление с именем отправителя (добавили chatId)
                        NotificationHelper.showMessageNotification(
                                MessageListenerService.this,
                                username,  // ← Имя в заголовке
                                messageText,  // ← Текст сообщения
                                chatId  // ← Новый параметр
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Если не удалось загрузить имя, показываем хоть что-то (добавили chatId)
                        NotificationHelper.showMessageNotification(
                                MessageListenerService.this,
                                "New Message",
                                messageText,
                                chatId
                        );
                    }
                });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}