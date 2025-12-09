package com.example.messenger;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.messenger.bottomnav.chats.ChatsFragment;
import com.example.messenger.bottomnav.new_chat.NewChatFragment;
import com.example.messenger.bottomnav.profile.ProfileFragment;
import com.example.messenger.databinding.ActivityMainBinding;
import com.example.messenger.notifications.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String currentUserId;

    private Fragment chatsFragment;
    private Fragment newChatFragment;
    private Fragment profileFragment;
    private Fragment currentFragment;

    private Handler handler = new Handler();
    private Runnable checkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!checkAuthentication()) {
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId != null) {
            setupPresence();
        }

        initFragments();
        setupAuthListener();

        // ВАЖНО: Запускаем сервис уведомлений
        requestNotificationPermissionIfNeeded();

        if (savedInstanceState == null) {
            loadFragment(chatsFragment);
            binding.bottomNav.setSelectedItemId(R.id.chats);
        }

        setupBottomNavigation();
        setupCheckTimer();
    }

    private boolean checkAuthentication() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return false;
        }
        return true;
    }

    private void initFragments() {
        chatsFragment = new ChatsFragment();
        newChatFragment = new NewChatFragment();
        profileFragment = new ProfileFragment();
        currentFragment = chatsFragment;
    }

    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                navigateToLogin();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    // ОБНОВЛЕНО: Правильная проверка и запрос разрешений
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Показываем объяснение перед запросом
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(this,
                            "Разрешите уведомления для получения новых сообщений",
                            Toast.LENGTH_LONG).show();
                }

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                // Разрешение уже есть
                startMessageService();
            }
        } else {
            // Android 12 и ниже - разрешение не требуется
            startMessageService();
        }
    }

    // НОВОЕ: Обработка результата запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show();
                startMessageService();
            } else {
                Toast.makeText(this,
                        "Уведомления отключены. Вы не будете получать оповещения о новых сообщениях",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (id == R.id.chats) {
                selectedFragment = chatsFragment;
            } else if (id == R.id.new_chat) {
                selectedFragment = newChatFragment;
            } else if (id == R.id.profile) {
                selectedFragment = profileFragment;
            }

            if (selectedFragment != null && selectedFragment != currentFragment) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();

        currentFragment = fragment;
    }

    // ОБНОВЛЕНО: Улучшенный запуск сервиса с проверками
    private void startMessageService() {
        try {
            Intent serviceIntent = new Intent(this, MessageListenerService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Log.d("MainActivity", "MessageListenerService started successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to start service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        // Останавливаем сервис при выходе
        stopService(new Intent(this, MessageListenerService.class));

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkIfDisabled() {
        if (currentUserId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId)
                .child("isDisabled")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean disabled = snapshot.getValue(Boolean.class);
                        if (disabled != null && disabled) {
                            Toast.makeText(MainActivity.this, "Your account has been banned", Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                            navigateToLogin();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void setupCheckTimer() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkIfDisabled();
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(checkRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }

        handler.removeCallbacks(checkRunnable);
        binding = null;
    }

    private void setupPresence() {
        if (currentUserId == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId);

        DatabaseReference connectedRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    userRef.child("online").setValue(true);
                    userRef.child("online").onDisconnect().setValue(false);
                    userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Presence setup error: " + error.getMessage());
            }
        });
    }
}