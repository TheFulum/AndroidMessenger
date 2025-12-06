package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

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
import java.util.Locale;

import com.example.messenger.databinding.ActivityChatBinding;
import com.example.messenger.message.Message;
import com.example.messenger.message.MessagesAdapter;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private String chatId;
    private String currentUserId;
    private ValueEventListener messagesListener;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra("chatId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        findAndLoadReceiverUsername();
        loadMessages();
    }

    private void setupUI() {
        // Кнопка назад
        binding.backBtn.setOnClickListener(v -> finish());

        // Кнопка отправки
        binding.sendMessageBtn.setOnClickListener(v -> sendMessage());
        updateSendButtonState(); // Изначально disabled

        // Отслеживание ввода текста для управления кнопкой отправки
        binding.messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Обработка Enter для отправки
        binding.messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    /**
     * Обновляет состояние кнопки отправки (активна только если есть текст)
     */
    private void updateSendButtonState() {
        boolean hasText = !binding.messageEt.getText().toString().trim().isEmpty();
        binding.sendMessageBtn.setEnabled(hasText);
        binding.sendMessageBtn.setAlpha(hasText ? 1.0f : 0.5f);
    }

    private void sendMessage() {
        String text = binding.messageEt.getText().toString().trim();
        if (text.isEmpty()) return;

        // Отключаем кнопку на время отправки
        binding.sendMessageBtn.setEnabled(false);

        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("ownerId", currentUserId);
        msg.put("date", dateFormat.format(new Date()));
        msg.put("timestamp", now);

        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push();

        msgRef.setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    // Обновляем lastMessageTime для сортировки чатов
                    updateLastMessage(text, now);
                    binding.messageEt.setText("");
                    updateSendButtonState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                    binding.sendMessageBtn.setEnabled(true);
                });
    }

    /**
     * Обновляет информацию о последнем сообщении в чате
     */
    private void updateLastMessage(String text, long timestamp) {
        String preview = text.length() > 50 ? text.substring(0, 47) + "..." : text;

        HashMap<String, Object> update = new HashMap<>();
        update.put("lastMessageTime", timestamp);
        update.put("lastMessagePreview", preview);

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .updateChildren(update);
    }

    private void findAndLoadReceiverUsername() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.chatUsernameTv.setText("Чат не найден");
                    return;
                }

                String user1 = snapshot.child("user1").getValue(String.class);
                String user2 = snapshot.child("user2").getValue(String.class);

                String receiverId = null;
                if (currentUserId.equals(user1)) {
                    receiverId = user2;
                } else if (currentUserId.equals(user2)) {
                    receiverId = user1;
                }

                if (receiverId == null) {
                    binding.chatUsernameTv.setText("Неизвестный");
                    return;
                }

                // Загружаем ник по receiverId
                loadUsername(receiverId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.chatUsernameTv.setText("Ошибка");
                Toast.makeText(ChatActivity.this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Загружает username пользователя по UID
     */
    private void loadUsername(String uid) {
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
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

    private void loadMessages() {
        messagesRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();

                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    String id = msgSnapshot.getKey();
                    String ownerId = msgSnapshot.child("ownerId").getValue(String.class);
                    String text = msgSnapshot.child("text").getValue(String.class);
                    String date = msgSnapshot.child("date").getValue(String.class);
                    Long timestamp = msgSnapshot.child("timestamp").getValue(Long.class);

                    if (text != null && ownerId != null) {
                        messages.add(new Message(
                                id,
                                ownerId,
                                text,
                                date != null ? date : "",
                                timestamp != null ? timestamp : 0L
                        ));
                    }
                }

                setupRecyclerView(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    /**
     * Настраивает RecyclerView со списком сообщений
     */
    private void setupRecyclerView(List<Message> messages) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.messagesRv.setLayoutManager(layoutManager);

        MessagesAdapter adapter = new MessagesAdapter(messages, chatId);
        binding.messagesRv.setAdapter(adapter);

        // Скролл к последнему сообщению
        if (adapter.getItemCount() > 0) {
            binding.messagesRv.post(() ->
                    binding.messagesRv.smoothScrollToPosition(adapter.getItemCount() - 1)
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Удаляем listener для предотвращения утечек памяти
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }

        // Очищаем binding
        binding = null;
    }
}