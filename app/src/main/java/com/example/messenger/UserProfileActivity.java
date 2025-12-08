package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.messenger.databinding.ActivityUserProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private String userId;
    private String currentUserId;
    private ValueEventListener userListener;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Получаем ID пользователя из Intent
        userId = getIntent().getStringExtra("userId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null || currentUserId == null) {
            Toast.makeText(this, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadUserData();
    }

    private void setupUI() {
        // Кнопка назад
        binding.backBtn.setOnClickListener(v -> finish());

        // Кнопка "Написать сообщение"
        binding.sendMessageBtn.setOnClickListener(v -> openOrCreateChat());

        // Клик на аватар для просмотра фото
        binding.profileImageView.setOnClickListener(v -> openMediaViewer());
    }

    private void openMediaViewer() {
        // Получаем URL фото профиля
        userRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Intent intent = new Intent(UserProfileActivity.this, MediaViewerActivity.class);
                    intent.putExtra("mediaUrl", imageUrl);
                    intent.putExtra("mediaType", "image");
                    intent.putExtra("title", binding.usernameTv.getText().toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(UserProfileActivity.this, "Нет фото профиля", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserData() {
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(UserProfileActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Имя пользователя
                String username = snapshot.child("username").getValue(String.class);
                binding.usernameTv.setText(username != null ? username : "Неизвестный");

                // Email
                String email = snapshot.child("email").getValue(String.class);
                binding.emailTv.setText(email != null ? email : "Не указано");

                // Телефон
                String phone = snapshot.child("phone").getValue(String.class);
                binding.phoneTv.setText(phone != null ? phone : "Не указано");

                // Дата рождения
                String birthday = snapshot.child("birthday").getValue(String.class);
                binding.birthdayTv.setText(birthday != null ? birthday : "Не указано");

                // Статус онлайн
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                updateUserStatus(isOnline, lastSeen);

                // Фото профиля
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    loadProfileImage(profileImageUrl);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private void updateUserStatus(Boolean isOnline, Long lastSeen) {
        if (isOnline != null && isOnline) {
            binding.statusTv.setText("В сети");
            binding.statusTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            binding.statusTv.setVisibility(View.VISIBLE);
        } else if (lastSeen != null && lastSeen > 0) {
            String timeAgo = getTimeAgo(lastSeen);
            binding.statusTv.setText("Был(а) в сети " + timeAgo);
            binding.statusTv.setTextColor(getResources().getColor(android.R.color.darker_gray));
            binding.statusTv.setVisibility(View.VISIBLE);
        } else {
            binding.statusTv.setVisibility(View.GONE);
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "только что";
        } else if (minutes < 60) {
            return minutes + " мин. назад";
        } else if (hours < 24) {
            return hours + " ч. назад";
        } else if (days < 7) {
            return days + " д. назад";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.username_icon)
                .error(R.drawable.username_icon)
                .into(binding.profileImageView);
    }

    private void openOrCreateChat() {
        // Проверяем существование чата
        String chatId1 = currentUserId + "_" + userId;
        String chatId2 = userId + "_" + currentUserId;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsRef.child(chatId1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot1) {
                if (snapshot1.exists()) {
                    // Чат существует с первым ID
                    openChat(chatId1);
                } else {
                    // Проверяем второй вариант ID
                    chatsRef.child(chatId2).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot2) {
                            if (snapshot2.exists()) {
                                // Чат существует со вторым ID
                                openChat(chatId2);
                            } else {
                                // Чата нет, создаем новый
                                createAndOpenChat(chatId1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(UserProfileActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAndOpenChat(String chatId) {
        HashMap<String, Object> chatData = new HashMap<>();
        chatData.put("user1", currentUserId);
        chatData.put("user2", userId);
        chatData.put("lastMessageTime", System.currentTimeMillis());
        chatData.put("lastMessagePreview", "");

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .setValue(chatData)
                .addOnSuccessListener(aVoid -> openChat(chatId))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                });
    }

    private void openChat(String chatId) {
        Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
        finish(); // Закрываем профиль после открытия чата
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }

        binding = null;
    }
}