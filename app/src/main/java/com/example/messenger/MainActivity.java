package com.example.messenger;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final Fragment chatsFragment = new ChatsFragment();
    private final Fragment newChatFragment = new NewChatFragment();
    private final Fragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Проверяем auth сразу
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Запрос permission для уведомлений (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                startMessageService();
            }
        } else {
            startMessageService();
        }

        // Загружаем начальный фрагмент
        loadFragment(chatsFragment);
        binding.bottomNav.setSelectedItemId(R.id.chats);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.chats) {
                loadFragment(chatsFragment);
                return true;
            }
            if (id == R.id.new_chat) {
                loadFragment(newChatFragment);
                return true;
            }
            if (id == R.id.profile) {
                loadFragment(profileFragment);
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
    }

    private void startMessageService() {
        Intent serviceIntent = new Intent(this, MessageListenerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMessageService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}