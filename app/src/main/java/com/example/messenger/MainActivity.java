package com.example.messenger;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

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

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private FirebaseAuth.AuthStateListener authStateListener;

    private Fragment chatsFragment;
    private Fragment newChatFragment;
    private Fragment profileFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Проверяем auth сразу
        if (!checkAuthentication()) {
            return;
        }

        // Инициализируем фрагменты
        initFragments();

        // Настраиваем auth listener для отслеживания выхода
        setupAuthListener();

        // Запрос permission для уведомлений
        requestNotificationPermissionIfNeeded();

        // Загружаем начальный фрагмент
        if (savedInstanceState == null) {
            loadFragment(chatsFragment);
            binding.bottomNav.setSelectedItemId(R.id.chats);
        }

        // Настраиваем bottom navigation
        setupBottomNavigation();
    }

    /**
     * Проверяет аутентификацию пользователя
     */
    private boolean checkAuthentication() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return false;
        }
        return true;
    }

    /**
     * Инициализирует все фрагменты заранее для избежания пересоздания
     */
    private void initFragments() {
        chatsFragment = new ChatsFragment();
        newChatFragment = new NewChatFragment();
        profileFragment = new ProfileFragment();
        currentFragment = chatsFragment;
    }

    /**
     * Настраивает AuthStateListener для отслеживания состояния авторизации
     */
    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                navigateToLogin();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    /**
     * Запрашивает разрешение на уведомления для Android 13+
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                startMessageService();
            }
        } else {
            startMessageService();
        }
    }

    /**
     * Настраивает bottom navigation с оптимизацией переключения фрагментов
     */
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

    /**
     * Загружает фрагмент
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();

        currentFragment = fragment;
    }

    /**
     * Запускает сервис для прослушивания новых сообщений
     */
    private void startMessageService() {
        try {
            Intent serviceIntent = new Intent(this, MessageListenerService.class);
            startService(serviceIntent);
        } catch (Exception e) {
            // Сервис может не запуститься по разным причинам, игнорируем
        }
    }

    /**
     * Переход на экран логина
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMessageService();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Дополнительная проверка при возврате в активити
        checkAuthentication();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Удаляем auth listener
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }

        // Очищаем binding
        binding = null;
    }
}