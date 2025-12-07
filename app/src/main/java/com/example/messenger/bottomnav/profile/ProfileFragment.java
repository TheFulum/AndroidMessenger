package com.example.messenger.bottomnav.profile;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.messenger.LoginActivity;
import com.example.messenger.R;
import com.example.messenger.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference userRef;
    private String currentUserId;
    private ProgressDialog progressDialog;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ValueEventListener userListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageToCloudinary(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        initializeCloudinary();
        setupViews();
        loadUserData();

        return binding.getRoot();
    }

    private void initializeCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dsfmj1rgd");
        config.put("api_key", "292327364799723");
        config.put("api_secret", "ViwIhwljI2owz0zxdFqVX4c8U58");

        try {
            MediaManager.init(requireContext(), config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupViews() {
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        // Клик на аватар для загрузки фото
        binding.profileImageView.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Кнопка выхода
        binding.logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // Редактирование имени
        binding.editUsernameBtn.setOnClickListener(v -> showEditDialog(
                "Изменить имя пользователя",
                binding.usernameTv.getText().toString(),
                "username",
                InputType.TYPE_CLASS_TEXT
        ));

        // Редактирование телефона
        binding.editPhoneBtn.setOnClickListener(v -> showEditDialog(
                "Изменить номер телефона",
                binding.phoneTv.getText().toString(),
                "phone",
                InputType.TYPE_CLASS_PHONE
        ));

        // Редактирование даты рождения
        binding.editBirthdayBtn.setOnClickListener(v -> showDatePickerDialog());

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Загрузка фото...");
        progressDialog.setCancelable(false);
    }

    private void loadUserData() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return; // защита от краша

                String username = snapshot.child("username").getValue(String.class);
                binding.usernameTv.setText(username != null ? username : "Не указано");

                String email = snapshot.child("email").getValue(String.class);
                binding.emailTv.setText(email != null ? email : "Не указано");

                String phone = snapshot.child("phone").getValue(String.class);
                binding.phoneTv.setText(phone != null ? phone : "Не указано");

                String birthday = snapshot.child("birthday").getValue(String.class);
                binding.birthdayTv.setText(birthday != null ? birthday : "Не указано");

                if (snapshot.hasChild("profileImageUrl")) {
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) loadProfileImage(imageUrl);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userRef.addValueEventListener(userListener);

    }

    private void showEditDialog(String title, String currentValue, String field, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        final EditText input = new EditText(requireContext());
        input.setInputType(inputType);
        input.setText(currentValue.equals("Не указано") ? "" : currentValue);
        input.setSelection(input.getText().length());
        input.setPadding(50, 30, 50, 30);

        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateField(field, newValue);
            } else {
                Toast.makeText(getContext(), "Поле не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear);
                    updateField("birthday", date);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void updateField(String field, String value) {
        userRef.child(field).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        progressDialog.show();

        MediaManager.get().upload(imageUri)
                .option("folder", "messenger_profiles")
                .option("public_id", "profile_" + currentUserId)
                .option("overwrite", true)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        progressDialog.dismiss();

                        String imageUrl = (String) resultData.get("secure_url");
                        saveImageUrlToFirebase(imageUrl);
                        loadProfileImage(imageUrl);

                        Toast.makeText(getContext(), "Фото загружено!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Ошибка: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveImageUrlToFirebase(String imageUrl) {
        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка сохранения URL", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.username_icon)
                .error(R.drawable.username_icon)
                .into(binding.profileImageView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        binding = null;
    }

}