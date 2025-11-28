package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra("chatId");

        if (chatId == null) {
            Toast.makeText(this, "Chat ID error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMessages(chatId);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.sendMessageBtn.performClick();
                return true;
            }
            return false;
        });

        binding.sendMessageBtn.setOnClickListener(v -> {
            String message = binding.messageEt.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.messageEt.setText("");

            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
            sendMessage(chatId, message, date);
        });
    }

    private void sendMessage(String chatId, String message, String date){
        HashMap<String, String> messageInfo = new HashMap<>();
        messageInfo.put("text", message);
        messageInfo.put("ownerId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        messageInfo.put("date", date);

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Chats")
                .child(chatId)
                .child("messages")
                .push()
                .setValue(messageInfo);
    }

    private void loadMessages(String chatId) {

        FirebaseDatabase.getInstance().getReference()
                .child("Chats")
                .child(chatId)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<Message> messages = new ArrayList<>();

                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            String id = messageSnapshot.getKey();
                            String ownerId = messageSnapshot.child("ownerId").getValue(String.class);
                            String text = messageSnapshot.child("text").getValue(String.class);
                            String date = messageSnapshot.child("date").getValue(String.class);

                            messages.add(new Message(id, ownerId, text, date));
                        }

                        // LayoutManager с автоскроллом вниз
                        LinearLayoutManager lm = new LinearLayoutManager(ChatActivity.this);
                        lm.setStackFromEnd(true);
                        binding.messagesRv.setLayoutManager(lm);

                        // адаптер
                        MessagesAdapter adapter = new MessagesAdapter(messages);
                        binding.messagesRv.setAdapter(adapter);

                        // форсируем скролл вниз после загрузки
                        binding.messagesRv.post(() -> {
                            if (adapter.getItemCount() > 0) {
                                binding.messagesRv.smoothScrollToPosition(adapter.getItemCount() - 1);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

}
