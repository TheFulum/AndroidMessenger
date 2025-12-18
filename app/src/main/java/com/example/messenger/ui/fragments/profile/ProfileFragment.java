package com.example.messenger.ui.fragments.profile;

import android.app.DatePickerDialog;
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
import com.example.messenger.ui.activities.LoginActivity;
import com.example.messenger.ui.activities.MediaViewerActivity;
import com.example.messenger.R;
import com.example.messenger.config.AppConfig;
import com.example.messenger.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference userRef;
    private String currentUserId;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ValueEventListener userListener;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        initializeCloudinary();
        setupViews();
        loadUserData();

        return binding.getRoot();
    }

    private void initializeCloudinary() {
        try {
            MediaManager.init(requireContext(), AppConfig.getCloudinaryConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupViews() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        binding.profileImageView.setOnClickListener(v -> showPhotoOptions());

        binding.logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });

        binding.editUsernameBtn.setOnClickListener(v -> showEditUsernameDialog());

        binding.editPhoneBtn.setOnClickListener(v -> showEditPhoneDialog());

        binding.editBirthdayBtn.setOnClickListener(v -> showDatePickerDialog());
    }

    private void loadUserData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        if (userListener != null && userRef != null) {
            try {
                userRef.removeEventListener(userListener);
            } catch (Exception ignored) {}
        }

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) {
                    return;
                }

                String username = snapshot.child("username").getValue(String.class);
                binding.usernameTv.setText(username != null ? username : "Not specified");

                String email = snapshot.child("email").getValue(String.class);
                binding.emailTv.setText(email != null ? email : "Not specified");

                String phone = snapshot.child("phone").getValue(String.class);
                binding.phoneTv.setText(phone != null ? phone : "Not specified");

                String birthday = snapshot.child("birthday").getValue(String.class);
                binding.birthdayTv.setText(birthday != null ? birthday : "Not specified");

                if (snapshot.hasChild("profileImageUrl")) {
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        loadProfileImage(imageUrl);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Data upload error", Toast.LENGTH_SHORT).show();
                }
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private void showEditUsernameDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change the user's name");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String currentUsername = binding.usernameTv.getText().toString();
        input.setText(currentUsername.equals("Not specified") ? "" : currentUsername);
        input.setSelection(input.getText().length());
        input.setPadding(50, 30, 50, 30);
        input.setHint("username");

        builder.setView(input);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newUsername = input.getText().toString().trim();
            validateAndUpdateUsername(newUsername, dialog);
        });
    }

    private void validateAndUpdateUsername(String username, AlertDialog dialog) {
        if (getContext() == null) return;

        if (username.isEmpty()) {
            Toast.makeText(getContext(), "The user name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() < 3) {
            Toast.makeText(getContext(), "Minimum of 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() > 20) {
            Toast.makeText(getContext(), "Maximum of 20 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            Toast.makeText(getContext(), "Only Latin letters, numbers and _", Toast.LENGTH_LONG).show();
            return;
        }

        checkUsernameUniqueness(username, dialog);
    }

    private void checkUsernameUniqueness(String username, AlertDialog dialog) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        Query query = FirebaseDatabase.getInstance()
                .getReference("Users")
                .orderByChild("username")
                .equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isUnique = true;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (!userSnapshot.getKey().equals(currentUserId)) {
                        isUnique = false;
                        break;
                    }
                }

                if (isUnique) {
                    updateField("username", username);
                    dialog.dismiss();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "This username is already taken", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Verification error. Try again later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEditPhoneDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change the phone number");

        com.example.messenger.utils.PhoneMaskEditText input =
                new com.example.messenger.utils.PhoneMaskEditText(requireContext());
        input.setPadding(50, 30, 50, 30);

        String currentPhone = binding.phoneTv.getText().toString();
        if (!"Not specified".equals(currentPhone)) {
            input.setPhone(currentPhone);
        }

        builder.setView(input);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (input.isComplete()) {
                String formattedPhone = input.getText().toString();
                updateField("phone", formattedPhone);
                dialog.dismiss();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Fill in the phone number completely", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDatePickerDialog() {
        if (getContext() == null) return;

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    Calendar minAgeDate = Calendar.getInstance();
                    minAgeDate.add(Calendar.YEAR, -13);

                    if (selectedDate.after(minAgeDate)) {
                        Toast.makeText(getContext(), "Minimum age: 13 years", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String date = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear);
                    updateField("birthday", date);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void updateField(String field, String value) {
        if (userRef == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: there is no connection to the database", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        userRef.child(field).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "The data has been updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Update error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri)
                .option("folder", AppConfig.CloudinaryFolders.PROFILE_IMAGES)
                .option("public_id", "profile_" + currentUserId)
                .option("overwrite", true)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveImageUrlToFirebase(imageUrl);
                        loadProfileImage(imageUrl);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Photo uploaded!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch();
    }

    private void saveImageUrlToFirebase(String imageUrl) {
        if (userRef == null) return;

        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error saving the URL", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProfileImage(String imageUrl) {
        if (binding == null) return;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.username_icon)
                .error(R.drawable.username_icon)
                .into(binding.profileImageView);
    }

    private void showPhotoOptions() {
        if (userRef == null) {
            imagePickerLauncher.launch("image/*");
            return;
        }

        userRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.getValue(String.class);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    String[] options = {"View a photo", "Edit a photo"};
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Profile Photo")
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) {
                                    openMediaViewer(imageUrl);
                                } else {
                                    imagePickerLauncher.launch("image/*");
                                }
                            })
                            .show();
                } else {
                    imagePickerLauncher.launch("image/*");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void openMediaViewer(String imageUrl) {
        if (getContext() == null) return;

        Intent intent = new Intent(requireContext(), MediaViewerActivity.class);
        intent.putExtra("mediaUrl", imageUrl);
        intent.putExtra("mediaType", "image");
        intent.putExtra("title", "My profile");
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            try {
                userRef.removeEventListener(userListener);
            } catch (Exception ignored) {}
        }
        binding = null;
    }
}