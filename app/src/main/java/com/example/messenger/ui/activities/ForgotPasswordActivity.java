package com.example.messenger.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messenger.R;
import com.example.messenger.databinding.ActivityForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupUI();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupUI() {
        updateResetButtonState();

        binding.emailEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateResetButtonState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.resetPasswordBtn.setOnClickListener(v -> {
            if (!isLoading) sendPasswordResetEmail();
        });

        binding.backBtn.setOnClickListener(v -> {
            if (!isLoading) finish();
        });
    }

    private void updateResetButtonState() {
        String email = binding.emailEt.getText().toString().trim();
        boolean isValid = !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

        binding.resetPasswordBtn.setEnabled(isValid && !isLoading);
        binding.resetPasswordBtn.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void sendPasswordResetEmail() {
        String email = binding.emailEt.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader(true);

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoader(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Письмо для сброса пароля отправлено на " + email,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Ошибка отправки письма";

                        if (errorMessage.contains("no user record")) {
                            errorMessage = "Пользователь с таким email не найден";
                        } else if (errorMessage.contains("network error")) {
                            errorMessage = "Ошибка сети. Проверьте подключение";
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoader(boolean show) {
        isLoading = show;
        binding.loaderBg.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loader.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.emailEt.setEnabled(!show);
        binding.resetPasswordBtn.setEnabled(!show);
        binding.backBtn.setEnabled(!show);

        if (!show) updateResetButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}