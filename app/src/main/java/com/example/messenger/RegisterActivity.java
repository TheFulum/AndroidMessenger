package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;
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
    private static final String TAG = "RegisterActivity";

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



        binding.signUpBtn.setOnClickListener(v ->  {
            if (binding.emailEt.getText().toString().isEmpty() || binding.passwordEt.getText().toString().isEmpty() || binding.usernameEt.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(),"Fields cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(binding.emailEt.getText().toString(), binding.passwordEt.getText().toString())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                HashMap<String, String> userInfo = new HashMap<>();
                                userInfo.put("email", binding.emailEt.getText().toString());
                                userInfo.put("username", binding.usernameEt.getText().toString());
                                userInfo.put("chats", "");


                                FirebaseDatabase.getInstance().getReference().child("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                                        .setValue(userInfo);

                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            }
                        });
            }
        });

        binding.signUpBackBtn.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));


    }
}