package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messenger.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.signUpBtn.setOnClickListener(v -> registerUser());

        binding.signUpBackBtn.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {

        String email = binding.emailEt.getText().toString().trim();
        String password = binding.passwordEt.getText().toString().trim();
        String username = binding.usernameEt.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader();

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = Objects.requireNonNull(
                                FirebaseAuth.getInstance().getCurrentUser()
                        ).getUid();

                        HashMap<String, Object> userInfo = new HashMap<>();
                        userInfo.put("uid", uid);
                        userInfo.put("username", username);
                        userInfo.put("email", email);

                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("Users")
                                .child(uid)
                                .setValue(userInfo)
                                .addOnSuccessListener(unused -> {

                                    hideLoader();

                                    Toast.makeText(this,
                                            "Account successfully created!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                });

                    } else {

                        hideLoader();

                        Toast.makeText(
                                this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showLoader() {
        binding.loaderBg.setVisibility(View.VISIBLE);
        binding.loader.setVisibility(View.VISIBLE);
        binding.getRoot().setEnabled(false);
    }

    private void hideLoader() {
        binding.loaderBg.setVisibility(View.GONE);
        binding.loader.setVisibility(View.GONE);
        binding.getRoot().setEnabled(true);
    }
}
