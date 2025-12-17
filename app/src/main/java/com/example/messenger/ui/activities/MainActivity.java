package com.example.messenger.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.messenger.R;
import com.example.messenger.ui.fragments.chats.ChatsFragment;
import com.example.messenger.ui.fragments.new_chat.NewChatFragment;
import com.example.messenger.ui.fragments.profile.ProfileFragment;
import com.example.messenger.databinding.ActivityMainBinding;
import com.example.messenger.services.MessageListenerService;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String myUid;
    private ValueEventListener unreadChatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Запуск сервиса уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, MessageListenerService.class));
        } else {
            startService(new Intent(this, MessageListenerService.class));
        }

        // Устанавливаем онлайн статус
        setUserOnlineStatus(true);

        // Настройка навигации
        loadFragment(new ChatsFragment());
        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.chats) {
                fragment = new ChatsFragment();
            } else if (itemId == R.id.new_chat) {
                fragment = new NewChatFragment();
            } else if (itemId == R.id.profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });

        // Настройка badge для непрочитанных чатов
        setupUnreadChatsBadge();
    }

    private void setupUnreadChatsBadge() {
        BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.chats);
        badge.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        badge.setBadgeTextColor(getResources().getColor(android.R.color.white));
        badge.setVisible(false);

        unreadChatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadChatsCount = 0;

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String user1 = chatSnap.child("user1").getValue(String.class);
                    String user2 = chatSnap.child("user2").getValue(String.class);

                    // Проверяем, участвует ли текущий пользователь в чате
                    if (myUid.equals(user1) || myUid.equals(user2)) {
                        Long unreadCount = chatSnap.child("unreadCount")
                                .child(myUid)
                                .getValue(Long.class);

                        if (unreadCount != null && unreadCount > 0) {
                            unreadChatsCount++;
                        }
                    }
                }

                // Обновляем badge
                if (unreadChatsCount > 0) {
                    badge.setNumber(unreadChatsCount);
                    badge.setVisible(true);
                } else {
                    badge.setVisible(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ошибка загрузки
            }
        };

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .addValueEventListener(unreadChatsListener);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void setUserOnlineStatus(boolean isOnline) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("online")
                .setValue(isOnline);

        if (!isOnline) {
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child("lastSeen")
                    .setValue(System.currentTimeMillis());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setUserOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Отписываемся от слушателя непрочитанных чатов
        if (unreadChatsListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Chats")
                    .removeEventListener(unreadChatsListener);
        }

        setUserOnlineStatus(false);
        binding = null;
    }
}