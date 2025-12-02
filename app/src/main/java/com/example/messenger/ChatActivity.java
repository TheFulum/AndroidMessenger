package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.widget.Toast;

import com.example.messenger.notifications.NotificationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.example.messenger.databinding.ActivityChatBinding;
import com.example.messenger.message.Message;
import com.example.messenger.message.MessagesAdapter;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private String chatId;
    private String currentUserId;
    private boolean isChatVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra("chatId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1. Определяем UID собеседника + загружаем его ник
        findAndLoadReceiverUsername();

        // 2. Загружаем сообщения
        loadMessages();

        // 3. Обработчики кнопок
        binding.backBtn.setOnClickListener(v -> finish());

        binding.sendMessageBtn.setOnClickListener(v -> sendMessage());

        binding.messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 6) { // IME_ACTION_SEND
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String text = binding.messageEt.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Сообщение не может быть пустым", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("ownerId", currentUserId);
        msg.put("date", date);

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push()
                .setValue(msg);

        binding.messageEt.setText("");
    }

    private void findAndLoadReceiverUsername() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String user1 = snapshot.child("user1").getValue(String.class);
                String user2 = snapshot.child("user2").getValue(String.class);

                String receiverId;

                if (currentUserId.equals(user1)) {
                    receiverId = user2;
                } else if (currentUserId.equals(user2)) {
                    receiverId = user1;
                } else {
                    binding.chatUsernameTv.setText("Неизвестный");
                    return;
                }

                // Теперь грузим ник по receiverId
                if (receiverId != null) {
                    FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(receiverId)
                            .child("username")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String username = snapshot.getValue(String.class);
                                    binding.chatUsernameTv.setText(username != null ? username : "Пользователь");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    binding.chatUsernameTv.setText("Ошибка");
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.chatUsernameTv.setText("Ошибка");
            }
        });
    }

    private void loadMessages() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                Message lastMsg = null;

                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    String id = msgSnapshot.getKey();
                    String ownerId = msgSnapshot.child("ownerId").getValue(String.class);
                    String text = msgSnapshot.child("text").getValue(String.class);
                    String date = msgSnapshot.child("date").getValue(String.class);

                    if (text != null) {
                        Message m = new Message(id, ownerId, text, date);
                        messages.add(m);
                        lastMsg = m;
                    }
                }

                LinearLayoutManager lm = new LinearLayoutManager(ChatActivity.this);
                lm.setStackFromEnd(true);
                binding.messagesRv.setLayoutManager(lm);

                MessagesAdapter adapter = new MessagesAdapter(messages);
                binding.messagesRv.setAdapter(adapter);

                if (adapter.getItemCount() > 0) {
                    binding.messagesRv.post(() ->
                            binding.messagesRv.smoothScrollToPosition(adapter.getItemCount() - 1)
                    );
                }

                // =========== УВЕДОМЛЕНИЕ, ЕСЛИ ЧАТ НЕ ОТКРЫТ ===========
                if (lastMsg != null &&
                        !lastMsg.getOwnerId().equals(currentUserId) && // не моё сообщение
                        !isChatVisible) { // chatActivity свернута
                    sendLocalNotification(lastMsg.getText());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendLocalNotification(String text) {
        NotificationUtils.createChannel(this);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
                        .setSmallIcon(R.drawable.message_icon)
                        .setContentTitle("New message")
                        .setContentText(text)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isChatVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isChatVisible = false;
    }


}