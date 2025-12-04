package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.messenger.bottomnav.chats.ChatsFragment;
import com.example.messenger.bottomnav.new_chat.NewChatFragment;
import com.example.messenger.bottomnav.profile.ProfileFragment;
import com.example.messenger.databinding.ActivityMainBinding;
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

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}
