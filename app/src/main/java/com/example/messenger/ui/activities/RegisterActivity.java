package com.example.messenger.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messenger.R;
import com.example.messenger.config.AppConfig;
import com.example.messenger.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        setupWindowInsets();
        setupUI();
        setupShowPasswordButton();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupUI() {
        updateSignUpButtonState();
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSignUpButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        binding.usernameEt.addTextChangedListener(textWatcher);
        binding.emailEt.addTextChangedListener(textWatcher);
        binding.passwordEt.addTextChangedListener(textWatcher);

        binding.signUpBtn.setOnClickListener(v -> {
            if (!isLoading) registerUser();
        });

        binding.signUpBackBtn.setOnClickListener(v -> {
            if (!isLoading) finish();
        });
    }

    private void setupShowPasswordButton() {
        binding.showPassBtn.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.showPassBtn.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.showPassBtn.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            binding.passwordEt.setSelection(binding.passwordEt.getText().length());
        });
    }

    private void updateSignUpButtonState() {
        String username = binding.usernameEt.getText().toString().trim();
        String email = binding.emailEt.getText().toString().trim();
        String password = binding.passwordEt.getText().toString().trim();
        boolean isValid = !username.isEmpty() && !email.isEmpty() && !password.isEmpty() && password.length() >= 6;
        binding.signUpBtn.setEnabled(isValid && !isLoading);
        binding.signUpBtn.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void registerUser() {
        String email = binding.emailEt.getText().toString().trim();
        String password = binding.passwordEt.getText().toString().trim();
        String username = binding.usernameEt.getText().toString().trim();

        if (username.length() < AppConfig.Firebase.MIN_USERNAME_LENGTH) {
            Toast.makeText(this, "Имя пользователя: минимум " +
                            AppConfig.Firebase.MIN_USERNAME_LENGTH + " символа",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() > AppConfig.Firebase.MAX_USERNAME_LENGTH) {
            Toast.makeText(this, "Имя пользователя: максимум " +
                            AppConfig.Firebase.MAX_USERNAME_LENGTH + " символов",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < AppConfig.Firebase.MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Пароль: минимум " +
                            AppConfig.Firebase.MIN_PASSWORD_LENGTH + " символов",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() > AppConfig.Firebase.MAX_PASSWORD_LENGTH) {
            Toast.makeText(this, "Пароль: максимум " +
                            AppConfig.Firebase.MAX_PASSWORD_LENGTH + " символов",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        checkUsernameAndRegister(username, email, password);
    }

    private void checkUsernameAndRegister(String username, String email, String password) {
        showLoader(true);
        Query query = FirebaseDatabase.getInstance()
                .getReference("Users")
                .orderByChild("username")
                .equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    showLoader(false);
                    Toast.makeText(RegisterActivity.this, "Это имя пользователя уже занято", Toast.LENGTH_SHORT).show();
                } else {
                    createFirebaseAccount(username, email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoader(false);
                Toast.makeText(RegisterActivity.this, "Ошибка проверки. Попробуйте позже", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createFirebaseAccount(String username, String email, String password) {
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        String uid = user.getUid();
                        // Зачеиним: сначала верификация, потом сохранение
                        sendEmailVerification(user, () -> {
                            // При успехе верификации - сохраняем в БД
                            saveUserToDatabase(uid, username, email, user);
                        });
                    } else {
                        showLoader(false);
                        String errorMessage = getString(task);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, Runnable onSuccess) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run(); // Продолжаем к сохранению без тоста здесь
                    } else {
                        showLoader(false);
                        Toast.makeText(this, "Не удалось отправить email для верификации. Попробуйте позже.", Toast.LENGTH_LONG).show();
                        user.delete(); // Очистка
                    }
                });
    }

    @NonNull
    private static String getString(Task<AuthResult> task) {
        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Ошибка регистрации";
        if (errorMessage.contains("email address is already in use")) {
            errorMessage = "Этот email уже зарегистрирован";
        } else if (errorMessage.contains("network error")) {
            errorMessage = "Ошибка сети. Проверьте подключение";
        } else if (errorMessage.contains("weak password")) {
            errorMessage = "Слишком простой пароль";
        }
        return errorMessage;
    }

    private void saveUserToDatabase(String uid, String username, String email, FirebaseUser user) {
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("uid", uid);
        userInfo.put("username", username);
        userInfo.put("email", email);
        userInfo.put("online", false);
        userInfo.put("lastSeen", 0L);
        userInfo.put("emailVerified", false);

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .setValue(userInfo)
                .addOnSuccessListener(unused -> {
                    showLoader(false);
                    Toast.makeText(this, "✅ Регистрация успешна!\n📧 Мы отправили письмо для подтверждения. Если не видно в inbox — проверьте спам.", Toast.LENGTH_LONG).show();
                    FirebaseAuth.getInstance().signOut();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    showLoader(false);
                    Toast.makeText(this, "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (user != null) {
                        user.delete(); // Очистка при ошибке сохранения
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoader(boolean show) {
        isLoading = show;
        binding.loaderBg.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loader.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.signUpBtn.setEnabled(!show);
        binding.usernameEt.setEnabled(!show);
        binding.emailEt.setEnabled(!show);
        binding.passwordEt.setEnabled(!show);
        binding.signUpBackBtn.setEnabled(!show);
        binding.showPassBtn.setEnabled(!show);
        if (!show) updateSignUpButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}