package com.example.messenger;

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

import com.example.messenger.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
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

    // Паттерн для валидации username
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
                // показать пароль
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.showPassBtn.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                // скрыть пароль
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.showPassBtn.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            // курсор в конец
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

        // ========== ВАЛИДАЦИЯ ==========

        // 1. Проверка на пустоту
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Валидация username
        if (username.length() < 3) {
            Toast.makeText(this, "Имя пользователя: минимум 3 символа", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() > 20) {
            Toast.makeText(this, "Имя пользователя: максимум 20 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            Toast.makeText(this, "Только латинские буквы, цифры и _", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Валидация email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Валидация пароля
        if (password.length() < 6) {
            Toast.makeText(this, "Пароль: минимум 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() > 30) {
            Toast.makeText(this, "Пароль: максимум 30 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем уникальность username перед регистрацией
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
                    // Username уже занят
                    showLoader(false);
                    Toast.makeText(RegisterActivity.this,
                            "Это имя пользователя уже занято",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Username свободен - регистрируем
                    createFirebaseAccount(username, email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoader(false);
                Toast.makeText(RegisterActivity.this,
                        "Ошибка проверки. Попробуйте позже",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createFirebaseAccount(String username, String email, String password) {
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        saveUserToDatabase(uid, username, email);
                    } else {
                        showLoader(false);
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Ошибка регистрации";

                        // Преобразуем типичные ошибки Firebase в понятный текст
                        if (errorMessage.contains("email address is already in use")) {
                            errorMessage = "Этот email уже зарегистрирован";
                        } else if (errorMessage.contains("network error")) {
                            errorMessage = "Ошибка сети. Проверьте подключение";
                        } else if (errorMessage.contains("weak password")) {
                            errorMessage = "Слишком простой пароль";
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String username, String email) {
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("uid", uid);
        userInfo.put("username", username);
        userInfo.put("email", email);
        userInfo.put("online", false);
        userInfo.put("lastSeen", 0L);

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .setValue(userInfo)
                .addOnSuccessListener(unused -> {
                    showLoader(false);
                    Toast.makeText(this, "Аккаунт успешно создан!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    showLoader(false);
                    Toast.makeText(this, "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Удаляем аккаунт из Auth, если не удалось сохранить в БД
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        FirebaseAuth.getInstance().getCurrentUser().delete();
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