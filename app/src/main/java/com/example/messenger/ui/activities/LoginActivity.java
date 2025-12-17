package com.example.messenger.ui.activities;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messenger.R;
import com.example.messenger.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityLoginBinding binding;
    private boolean isPasswordVisible = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            showLoader(true); // Пока проверяем
            checkEmailVerification(currentUser); // Переиспользуем метод, он сам navigate или покажет диалог
            return; // Не продолжаем setup UI, если уже logged
        }
        // Если не logged — setup UI
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

        // 🔥 НОВАЯ КНОПКА: Forgot Password
        binding.forgotPasswordTv.setOnClickListener(v -> {
            if (!isLoading)
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void setupShowPasswordButton() {
        ImageButton showPassBtn = binding.showPassBtn;

        showPassBtn.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;

            if (isPasswordVisible) {
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                showPassBtn.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                binding.passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                showPassBtn.setImageResource(R.drawable.baseline_visibility_off_24);
            }

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
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader(true);

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (user != null) {
                            // 🔥 ПРОВЕРЯЕМ ВЕРИФИКАЦИЮ EMAIL
                            checkEmailVerification(user);
                        }
                    } else {
                        showLoader(false);
                        Exception e = task.getException();

                        String errorMessage = "Ошибка входа";
                        if (e != null && e.getMessage() != null) {
                            if (e.getMessage().contains("disabled")) {
                                errorMessage = "Аккаунт заблокирован администрацией";
                            } else if (e.getMessage().contains("no user record") ||
                                    e.getMessage().contains("invalid-credential")) {
                                errorMessage = "Неверный email или пароль";
                            } else if (e.getMessage().contains("wrong-password")) {
                                errorMessage = "Неверный пароль";
                            } else if (e.getMessage().contains("network error")) {
                                errorMessage = "Ошибка сети. Проверьте подключение";
                            } else {
                                errorMessage = e.getMessage();
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser user) {
        user.reload().addOnCompleteListener(reloadTask -> {
            if (reloadTask.isSuccessful()) {
                if (user.isEmailVerified()) {
                    updateEmailVerificationStatus(user.getUid(), true);
                    requestNotificationPermissionIfNeeded();
                    showLoader(false);
                    navigateToMain();
                } else {
                    showLoader(false);
                    showEmailNotVerifiedDialog(user);
                }
            } else {
                showLoader(false);
                Toast.makeText(this, "Ошибка проверки статуса верификации", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmailNotVerifiedDialog(FirebaseUser user) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email не подтвержден")
                .setMessage("Ваш email еще не подтвержден. Проверьте почту и перейдите по ссылке в письме.\n\nЕсли письмо не пришло, мы можем отправить его повторно.")
                .setPositiveButton("Отправить снова", (dialog, which) -> {
                    resendVerificationEmail(user);
                })
                .setNegativeButton("Позже", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void resendVerificationEmail(FirebaseUser user) {
        showLoader(true);

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoader(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "✅ Письмо отправлено! Проверьте почту",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Не удалось отправить письмо. Попробуйте позже",
                                Toast.LENGTH_SHORT).show();
                    }

                    FirebaseAuth.getInstance().signOut();
                });
    }

    private void updateEmailVerificationStatus(String uid, boolean verified) {
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("emailVerified")
                .setValue(verified)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Можно тост или лог, но не критично — не стопим navigate
                        Toast.makeText(this, "Ошибка обновления статуса верификации в БД", Toast.LENGTH_SHORT).show();
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
        binding.forgotPasswordTv.setEnabled(!show);

        if (!show) updateLoginButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}