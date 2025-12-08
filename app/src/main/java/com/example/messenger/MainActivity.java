package com.example.messenger;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

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
            setUserOnline();
        }

        initFragments();
        setupAuthListener();
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

    private void startMessageService() {
        try {
            Intent serviceIntent = new Intent(this, MessageListenerService.class);
            startService(serviceIntent);
        } catch (Exception ignored) {}
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setUserOnline() {
        if (currentUserId == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("online", true);
        status.put("lastSeen", ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId)
                .updateChildren(status);
    }

    private void setUserOffline() {
        if (currentUserId == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("online", false);
        status.put("lastSeen", ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId)
                .updateChildren(status);
    }

    // 🔥 Проверка disabled аккаунта
    private void checkIfDisabled() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true)
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Your account has been banned", Toast.LENGTH_LONG).show();
                    FirebaseAuth.getInstance().signOut();
                    navigateToLogin();
                });
    }

    private void setupCheckTimer() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkIfDisabled();
                handler.postDelayed(this, 15000); // каждые 15 секунд
            }
        };
        handler.post(checkRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthentication();
        checkIfDisabled();

        if (currentUserId != null) setUserOnline();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (currentUserId != null) setUserOffline();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }

        if (currentUserId != null) setUserOffline();

        handler.removeCallbacks(checkRunnable);
        binding = null;
    }
}
