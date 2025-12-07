package com.example.messenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messenger.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityLoginBinding binding;
    private boolean isPasswordVisible = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupUI();
        setupShowPasswordButton();
    }

    private boolean isUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null;
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupUI() {
        updateLoginButtonState();

        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.emailEt.addTextChangedListener(tw);
        binding.passwordEt.addTextChangedListener(tw);

        binding.loginBtn.setOnClickListener(v -> {
            if (!isLoading) loginUser();
        });

        binding.goToRegisterActivityTv.setOnClickListener(v -> {
            if (!isLoading)
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void setupShowPasswordButton() {
        ImageButton showPassBtn = binding.showPassBtn;

        showPassBtn.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;

            if (isPasswordVisible) {
                // показать пароль
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                showPassBtn.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                // скрыть пароль
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                showPassBtn.setImageResource(R.drawable.baseline_visibility_off_24);
            }

            // курсор в конец
            binding.passwordEt.setSelection(binding.passwordEt.getText().length());
        });
    }

    private void updateLoginButtonState() {
        String email = binding.emailEt.getText().toString().trim();
        String password = binding.passwordEt.getText().toString().trim();

        boolean ok = !email.isEmpty() && !password.isEmpty() && password.length() >= 6;
        binding.loginBtn.setEnabled(ok && !isLoading);
        binding.loginBtn.setAlpha(ok ? 1f : 0.5f);
    }

    private void loginUser() {
        String email = binding.emailEt.getText().toString().trim();
        String password = binding.passwordEt.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader(true);

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoader(false);

                    if (task.isSuccessful()) {
                        requestNotificationPermissionIfNeeded();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void navigateToMain() {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showLoader(boolean show) {
        isLoading = show;

        binding.loaderBg.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loader.setVisibility(show ? View.VISIBLE : View.GONE);

        binding.emailEt.setEnabled(!show);
        binding.passwordEt.setEnabled(!show);
        binding.showPassBtn.setEnabled(!show);
        binding.loginBtn.setEnabled(!show);
        binding.goToRegisterActivityTv.setEnabled(!show);

        if (!show) updateLoginButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
