package com.example.messenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.messenger.databinding.ActivityChatBinding;
import com.example.messenger.message.Message;
import com.example.messenger.message.MessagesAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private String chatId;
    private String currentUserId;
    private String receiverId;
    private ValueEventListener messagesListener;
    private DatabaseReference messagesRef;
    private MessagesAdapter messagesAdapter;
    private LinearLayoutManager layoutManager;

    private boolean isUploading = false;
    private String pendingFileName = "";
    private boolean isAtBottom = true;
    private int newMessagesCount = 0;

    // Голосовые сообщения
    private MediaRecorder mediaRecorder;
    private String voiceFilePath;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private Handler recordingHandler = new Handler(Looper.getMainLooper());
    private Runnable recordingRunnable;

    private ActivityResultLauncher<String> filePickerLauncher;

    private ValueEventListener userStatusListener;
    private Message replyingToMessage = null;
    private String replyingToOwnerName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Регистрация launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleFileSelection(uri);
                    }
                }
        );

        chatId = getIntent().getStringExtra("chatId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeCloudinary();
        setupUI();
        setupChatMenu();
        findAndLoadReceiverData();
        loadMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetUnreadCount();
        markMessagesAsRead();
    }

    private void resetUnreadCount() {
        if (chatId == null || currentUserId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("unreadCount")
                .child(currentUserId)
                .setValue(0);
    }

    private void initializeCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dsfmj1rgd");
        config.put("api_key", "292327364799723");
        config.put("api_secret", "ViwIhwljI2owz0zxdFqVX4c8U58");

        try {
            MediaManager.init(this, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        binding.backBtn.setOnClickListener(v -> finish());

        View.OnClickListener openProfileListener = v -> {
            if (receiverId != null) {
                Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", receiverId);
                startActivity(intent);
            }
        };

        binding.chatProfileImage.setOnClickListener(openProfileListener);
        binding.chatUsernameTv.setOnClickListener(openProfileListener);

        binding.attachFileBtn.setOnClickListener(v -> showFileTypeDialog());

        binding.sendMessageBtn.setOnClickListener(v -> sendMessage());
        updateSendButtonState();

        binding.messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();

                // Переключаем между кнопкой отправки и микрофоном
                if (s.length() > 0) {
                    binding.sendMessageBtn.setVisibility(View.VISIBLE);
                    binding.voiceRecordBtn.setVisibility(View.GONE);
                } else {
                    binding.sendMessageBtn.setVisibility(View.GONE);
                    binding.voiceRecordBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Автоскролл при фокусе на EditText (когда открывается клавиатура)
        binding.messageEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.messagesRv.postDelayed(this::scrollToBottom, 300);
            }
        });

        // Кнопка scroll to bottom
        binding.scrollToBottomFab.setOnClickListener(v -> {
            scrollToBottom();
            newMessagesCount = 0;
        });

        // Голосовые сообщения
        binding.voiceRecordBtn.setOnClickListener(v -> startVoiceRecording());
        binding.cancelRecordingBtn.setOnClickListener(v -> cancelVoiceRecording());
        binding.sendVoiceBtn.setOnClickListener(v -> sendVoiceMessage());

        // Слушатель скролла для показа/скрытия кнопки
        setupScrollListener();
    }

    private void showFileTypeDialog() {
        String[] options = {"📷 Фото", "🎥 Видео", "📄 Документ"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите тип файла")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        filePickerLauncher.launch("image/*");
                    } else if (which == 1) {
                        filePickerLauncher.launch("video/*");
                    } else {
                        filePickerLauncher.launch("*/*");
                    }
                })
                .show();
    }

    private void handleFileSelection(Uri uri) {
        String fileName = getFileName(uri);
        long fileSize = getFileSize(uri);
        String mimeType = getContentResolver().getType(uri);

        if (fileName == null) {
            Toast.makeText(this, "Ошибка получения имени файла", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка размера файла (максимум 100 MB для Cloudinary free plan)
        long maxFileSize = 100 * 1024 * 1024; // 100 MB в байтах
        if (fileSize > maxFileSize) {
            String sizeMB = String.format("%.1f", fileSize / (1024.0 * 1024.0));
            Toast.makeText(this,
                    "Файл слишком большой (" + sizeMB + " MB). Максимум 100 MB",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String fileType = "document";
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                fileType = "image";
            } else if (mimeType.startsWith("video/")) {
                fileType = "video";
            }
        }

        // Для видео получаем длительность
        long videoDuration = 0;
        if (fileType.equals("video")) {
            videoDuration = getVideoDuration(uri);
        }

        uploadFileToCloudinary(uri, fileName, fileSize, fileType, videoDuration);
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return fileName != null ? fileName : "file_" + System.currentTimeMillis();
    }

    private long getFileSize(Uri uri) {
        long fileSize = 0;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1) {
                fileSize = cursor.getLong(sizeIndex);
            }
            cursor.close();
        }
        return fileSize;
    }


    private void uploadFileToCloudinary(Uri fileUri, String fileName, long fileSize, String fileType, long videoDuration) {
        if (isUploading) {
            Toast.makeText(this, "Дождитесь завершения предыдущей загрузки", Toast.LENGTH_SHORT).show();
            return;
        }

        isUploading = true;
        pendingFileName = fileName;

        binding.uploadProgressContainer.setVisibility(View.VISIBLE);
        binding.uploadFileNameTv.setText(fileName);
        binding.uploadProgressBar.setProgress(0);
        binding.uploadProgressTv.setText("Загрузка... 0%");

        binding.attachFileBtn.setEnabled(false);
        binding.sendMessageBtn.setEnabled(false);

        String folder;
        if (fileType.equals("image")) {
            folder = "messenger_images";
        } else if (fileType.equals("video")) {
            folder = "messenger_videos";
        } else {
            folder = "messenger_files";
        }

        String publicId = "file_" + System.currentTimeMillis();

        MediaManager.get().upload(fileUri)
                .option("folder", folder)
                .option("public_id", publicId)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        runOnUiThread(() -> {
                            binding.uploadProgressBar.setProgress(progress);
                            binding.uploadProgressTv.setText("Загрузка... " + progress + "%");
                        });
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");

                        runOnUiThread(() -> {
                            binding.uploadProgressContainer.setVisibility(View.GONE);
                            isUploading = false;
                            binding.attachFileBtn.setEnabled(true);
                            updateSendButtonState();

                            sendMessageWithFile(fileUrl, fileName, fileSize, fileType, videoDuration);
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            binding.uploadProgressContainer.setVisibility(View.GONE);
                            isUploading = false;
                            binding.attachFileBtn.setEnabled(true);
                            updateSendButtonState();

                            Toast.makeText(ChatActivity.this,
                                    "Ошибка загрузки: " + error.getDescription(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void sendMessageWithFile(String fileUrl, String fileName, long fileSize, String fileType, long videoDuration) {
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        String text = binding.messageEt.getText().toString().trim();

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", text.isEmpty() ? "" : text);
        msg.put("ownerId", currentUserId);
        msg.put("date", dateFormat.format(new Date()));
        msg.put("timestamp", now);
        msg.put("fileUrl", fileUrl);
        msg.put("fileType", fileType);
        msg.put("fileName", fileName);
        msg.put("fileSize", fileSize);

        if (fileType.equals("video") && videoDuration > 0) {
            msg.put("videoDuration", videoDuration);
        }

        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push();

        msgRef.setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    String preview;
                    if (fileType.equals("image")) {
                        preview = "📷 Фото";
                    } else if (fileType.equals("video")) {
                        preview = "🎥 Видео";
                    } else {
                        preview = "📄 " + fileName;
                    }
                    updateLastMessage(preview, now);
                    incrementUnreadCount();
                    binding.messageEt.setText("");
                    scrollToBottom();
                    Toast.makeText(this, "Файл отправлен", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSendButtonState() {
        boolean hasText = !binding.messageEt.getText().toString().trim().isEmpty();
        binding.sendMessageBtn.setEnabled(hasText && !isUploading);
        binding.sendMessageBtn.setAlpha((hasText && !isUploading) ? 1.0f : 0.5f);
    }

    // Добавьте этот метод
    private void incrementUnreadCount() {
        if (receiverId == null) return;

        DatabaseReference unreadRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("unreadCount")
                .child(receiverId);

        unreadRef.get().addOnSuccessListener(snapshot -> {
            Long currentCount = snapshot.getValue(Long.class);
            int newCount = (currentCount != null ? currentCount.intValue() : 0) + 1;
            unreadRef.setValue(newCount);
        });
    }

    private void scrollToBottom() {
        if (messagesAdapter != null && messagesAdapter.getItemCount() > 0) {
            binding.messagesRv.smoothScrollToPosition(messagesAdapter.getItemCount() - 1);
            binding.scrollToBottomFab.setVisibility(View.GONE);
            isAtBottom = true;
        }
    }

    private void setupScrollListener() {
        binding.messagesRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();

                    // Проверяем, находимся ли мы внизу списка
                    isAtBottom = (lastVisiblePosition >= totalItemCount - 2);

                    // Показываем/скрываем кнопку
                    if (isAtBottom) {
                        binding.scrollToBottomFab.setVisibility(View.GONE);
                        newMessagesCount = 0;
                    } else {
                        binding.scrollToBottomFab.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void updateLastMessage(String text, long timestamp) {
        String preview = text.length() > 50 ? text.substring(0, 47) + "..." : text;

        HashMap<String, Object> update = new HashMap<>();
        update.put("lastMessageTime", timestamp);
        update.put("lastMessagePreview", preview);

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .updateChildren(update);
    }

    private void findAndLoadReceiverData() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.chatUsernameTv.setText("Чат не найден");
                    return;
                }

                String user1 = snapshot.child("user1").getValue(String.class);
                String user2 = snapshot.child("user2").getValue(String.class);

                if (currentUserId.equals(user1)) {
                    receiverId = user2;
                } else if (currentUserId.equals(user2)) {
                    receiverId = user1;
                }

                if (receiverId == null) {
                    binding.chatUsernameTv.setText("Неизвестный");
                    return;
                }

                loadReceiverData(receiverId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.chatUsernameTv.setText("Ошибка");
            }
        });
    }

    private void loadReceiverData(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid);

        // Используем addValueEventListener для real-time обновлений статуса
        userStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                binding.chatUsernameTv.setText(username != null ? username : "Пользователь");

                // Обновляем статус пользователя
                updateUserStatus(isOnline, lastSeen);

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    loadProfileImage(profileImageUrl);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.chatUsernameTv.setText("Ошибка");
            }
        };

        userRef.addValueEventListener(userStatusListener);
    }

    private void updateUserStatus(Boolean isOnline, Long lastSeen) {
        if (isOnline != null && isOnline) {
            binding.chatStatusTv.setText("В сети");
            binding.chatStatusTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            binding.chatStatusTv.setVisibility(View.VISIBLE);
        } else if (lastSeen != null && lastSeen > 0) {
            String timeAgo = getTimeAgo(lastSeen);
            binding.chatStatusTv.setText("Был(а) в сети " + timeAgo);
            binding.chatStatusTv.setTextColor(getResources().getColor(android.R.color.darker_gray));
            binding.chatStatusTv.setVisibility(View.VISIBLE);
        } else {
            binding.chatStatusTv.setVisibility(View.GONE);
        }
    }


    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "только что";
        } else if (minutes < 60) {
            return minutes + " мин. назад";
        } else if (hours < 24) {
            return hours + " ч. назад";
        } else if (days < 7) {
            return days + " д. назад";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .into(binding.chatProfileImage);
    }

    private void loadMessages() {
        messagesRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();

                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    String id = msgSnapshot.getKey();
                    String ownerId = msgSnapshot.child("ownerId").getValue(String.class);
                    String text = msgSnapshot.child("text").getValue(String.class);
                    String date = msgSnapshot.child("date").getValue(String.class);
                    Long timestamp = msgSnapshot.child("timestamp").getValue(Long.class);

                    String fileUrl = msgSnapshot.child("fileUrl").getValue(String.class);
                    String fileType = msgSnapshot.child("fileType").getValue(String.class);
                    String fileName = msgSnapshot.child("fileName").getValue(String.class);
                    Long fileSize = msgSnapshot.child("fileSize").getValue(Long.class);
                    Long voiceDuration = msgSnapshot.child("voiceDuration").getValue(Long.class);
                    Long videoDuration = msgSnapshot.child("videoDuration").getValue(Long.class);

                    Boolean isForwarded = msgSnapshot.child("isForwarded").getValue(Boolean.class);
                    String forwardedFrom = msgSnapshot.child("forwardedFrom").getValue(String.class);
                    Boolean isEdited = msgSnapshot.child("isEdited").getValue(Boolean.class);

                    String replyToMessageId = msgSnapshot.child("replyToMessageId").getValue(String.class);
                    String replyToText = msgSnapshot.child("replyToText").getValue(String.class);
                    String replyToOwnerName = msgSnapshot.child("replyToOwnerName").getValue(String.class);
                    String replyToFileType = msgSnapshot.child("replyToFileType").getValue(String.class);

                    // Загружаем статус прочитанности
                    Boolean read = msgSnapshot.child("read").getValue(Boolean.class);

                    // НОВОЕ: Загружаем данные контакта
                    String contactUserId = msgSnapshot.child("contactUserId").getValue(String.class);
                    String contactUsername = msgSnapshot.child("contactUsername").getValue(String.class);

                    if (ownerId != null) {
                        Message message = new Message(
                                id,
                                ownerId,
                                text != null ? text : "",
                                date != null ? date : "",
                                timestamp != null ? timestamp : 0L,
                                fileUrl,
                                fileType,
                                fileName,
                                fileSize != null ? fileSize : 0L,
                                voiceDuration != null ? voiceDuration : 0L,
                                videoDuration != null ? videoDuration : 0L
                        );

                        if (isForwarded != null && isForwarded) {
                            message.setForwarded(true);
                            if (forwardedFrom != null) {
                                message.setForwardedFrom(forwardedFrom);
                            }
                        }

                        message.setEdited(isEdited != null && isEdited);

                        if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
                            message.setReplyToMessageId(replyToMessageId);
                            message.setReplyToText(replyToText);
                            message.setReplyToOwnerName(replyToOwnerName);
                            message.setReplyToFileType(replyToFileType);
                        }

                        // Устанавливаем статус прочитанности
                        message.setRead(read != null && read);

                        // НОВОЕ: Устанавливаем данные контакта
                        if (contactUserId != null && !contactUserId.isEmpty()) {
                            message.setContactUserId(contactUserId);
                            message.setContactUsername(contactUsername);
                        }

                        messages.add(message);
                    }
                }

                setupRecyclerView(messages);

                // Отмечаем сообщения как прочитанные при открытии чата
                markMessagesAsRead();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void setupRecyclerView(List<Message> messages) {
        if (layoutManager == null) {
            layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            binding.messagesRv.setLayoutManager(layoutManager);
        }

        boolean wasAtBottom = isAtBottom;
        int previousItemCount = messagesAdapter != null ? messagesAdapter.getItemCount() : 0;

        if (messagesAdapter == null) {
            messagesAdapter = new MessagesAdapter(messages, chatId);
            binding.messagesRv.setAdapter(messagesAdapter);
        } else {
            messagesAdapter.updateMessages(messages);
        }

        // Автоскролл только если пользователь был внизу или это первая загрузка
        if (messagesAdapter.getItemCount() > 0) {
            if (wasAtBottom || previousItemCount == 0) {
                binding.messagesRv.post(() ->
                        binding.messagesRv.smoothScrollToPosition(messagesAdapter.getItemCount() - 1)
                );
            } else {
                // Если не внизу - увеличиваем счетчик новых сообщений
                int newCount = messagesAdapter.getItemCount() - previousItemCount;
                if (newCount > 0) {
                    newMessagesCount += newCount;
                    binding.scrollToBottomFab.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }

        // ВАЖНО: Отписываемся от слушателя статуса
        if (receiverId != null && userStatusListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(receiverId)
                    .removeEventListener(userStatusListener);
        }

        if (isRecording) {
            stopRecording();
        }

        binding = null;
    }

    // ========== Голосовые сообщения ==========

    private void startVoiceRecording() {
        // Проверяем разрешение на запись
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 100);
            return;
        }

        try {
            // Создаем файл для записи
            voiceFilePath = getCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(voiceFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // Показываем UI записи
            binding.messageInputContainer.setVisibility(View.GONE);
            binding.voiceRecordingContainer.setVisibility(View.VISIBLE);

            // Запускаем таймер
            startRecordingTimer();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка записи: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecordingTimer() {
        recordingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    int seconds = (int) (elapsed / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;

                    binding.recordingTimeTv.setText(String.format("%d:%02d", minutes, seconds));

                    recordingHandler.postDelayed(this, 100);
                }
            }
        };
        recordingHandler.post(recordingRunnable);
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
        }

        isRecording = false;
        recordingHandler.removeCallbacks(recordingRunnable);

        // Возвращаем обычный UI
        binding.voiceRecordingContainer.setVisibility(View.GONE);
        binding.messageInputContainer.setVisibility(View.VISIBLE);
    }

    private void cancelVoiceRecording() {
        stopRecording();

        // Удаляем файл
        if (voiceFilePath != null) {
            new java.io.File(voiceFilePath).delete();
            voiceFilePath = null;
        }

        Toast.makeText(this, "Запись отменена", Toast.LENGTH_SHORT).show();
    }

    private void sendVoiceMessage() {
        if (voiceFilePath == null) return;

        stopRecording();

        long duration = System.currentTimeMillis() - recordingStartTime;

        // Загружаем на Cloudinary
        uploadVoiceToCloudinary(Uri.fromFile(new java.io.File(voiceFilePath)), duration);
    }

    private void uploadVoiceToCloudinary(Uri voiceUri, long duration) {
        binding.uploadProgressContainer.setVisibility(View.VISIBLE);
        binding.uploadFileNameTv.setText("Голосовое сообщение");
        binding.uploadProgressBar.setProgress(0);
        binding.uploadProgressTv.setText("Загрузка... 0%");

        binding.attachFileBtn.setEnabled(false);
        binding.voiceRecordBtn.setEnabled(false);

        String publicId = "voice_" + System.currentTimeMillis();

        MediaManager.get().upload(voiceUri)
                .option("folder", "messenger_voices")
                .option("public_id", publicId)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        runOnUiThread(() -> {
                            binding.uploadProgressBar.setProgress(progress);
                            binding.uploadProgressTv.setText("Загрузка... " + progress + "%");
                        });
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");

                        runOnUiThread(() -> {
                            binding.uploadProgressContainer.setVisibility(View.GONE);
                            binding.attachFileBtn.setEnabled(true);
                            binding.voiceRecordBtn.setEnabled(true);

                            sendVoiceMessageToFirebase(fileUrl, duration);

                            // Удаляем локальный файл
                            if (voiceFilePath != null) {
                                new java.io.File(voiceFilePath).delete();
                                voiceFilePath = null;
                            }
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            binding.uploadProgressContainer.setVisibility(View.GONE);
                            binding.attachFileBtn.setEnabled(true);
                            binding.voiceRecordBtn.setEnabled(true);

                            Toast.makeText(ChatActivity.this,
                                    "Ошибка загрузки: " + error.getDescription(),
                                    Toast.LENGTH_SHORT).show();

                            // Удаляем локальный файл
                            if (voiceFilePath != null) {
                                new java.io.File(voiceFilePath).delete();
                                voiceFilePath = null;
                            }
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void sendVoiceMessageToFirebase(String voiceUrl, long duration) {
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", "");
        msg.put("ownerId", currentUserId);
        msg.put("date", dateFormat.format(new Date()));
        msg.put("timestamp", now);
        msg.put("fileUrl", voiceUrl);
        msg.put("fileType", "voice");
        msg.put("fileName", "voice.m4a");
        msg.put("fileSize", 0L);
        msg.put("voiceDuration", duration);

        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push();

        msgRef.setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    updateLastMessage("🎤 Голосовое сообщение", now);
                    incrementUnreadCount();
                    scrollToBottom();
                    Toast.makeText(this, "Голосовое сообщение отправлено", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                });
    }

    private long getVideoDuration(Uri videoUri) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(this, videoUri);
            String duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            return duration != null ? Long.parseLong(duration) : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void showReplyBlock(Message message, String ownerName) {
        replyingToMessage = message;
        replyingToOwnerName = ownerName;

        binding.replyContainer.setVisibility(View.VISIBLE);
        binding.replyOwnerNameTv.setText(ownerName);

        // Определяем текст для отображения
        String displayText;
        if (message.hasFile()) {
            if (message.isImage()) {
                displayText = "📷 Фото";
            } else if (message.isVideo()) {
                displayText = "🎥 Видео";
            } else if (message.isVoice()) {
                displayText = "🎤 Голосовое сообщение";
            } else {
                displayText = "📄 " + message.getFileName();
            }
        } else {
            displayText = message.getText();
        }

        binding.replyTextTv.setText(displayText);

        // Фокус на поле ввода
        binding.messageEt.requestFocus();
    }

    // Отменить ответ
    private void cancelReply() {
        replyingToMessage = null;
        replyingToOwnerName = null;
        binding.replyContainer.setVisibility(View.GONE);
    }

    // Обновленный метод sendMessage с поддержкой ответов
    private void sendMessage() {
        String text = binding.messageEt.getText().toString().trim();
        if (text.isEmpty()) return;

        binding.sendMessageBtn.setEnabled(false);

        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("ownerId", currentUserId);
        msg.put("date", dateFormat.format(new Date()));
        msg.put("timestamp", now);

        // НОВОЕ: Добавляем данные об ответе
        if (replyingToMessage != null) {
            msg.put("replyToMessageId", replyingToMessage.getId());
            msg.put("replyToOwnerName", replyingToOwnerName);

            // Сохраняем текст или тип файла
            if (replyingToMessage.hasFile()) {
                msg.put("replyToFileType", replyingToMessage.getFileType());
                if (replyingToMessage.isDocument()) {
                    msg.put("replyToText", replyingToMessage.getFileName());
                } else {
                    msg.put("replyToText", "");
                }
            } else {
                msg.put("replyToText", replyingToMessage.getText());
            }
        }

        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push();

        msgRef.setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    updateLastMessage(text, now);
                    incrementUnreadCount();
                    binding.messageEt.setText("");
                    updateSendButtonState();
                    cancelReply();  // Закрываем блок ответа
                    scrollToBottom();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                    binding.sendMessageBtn.setEnabled(true);
                });
    }

    public void scrollToMessage(int position) {
        if (binding == null || binding.messagesRv == null) return;

        // Плавная прокрутка
        binding.messagesRv.smoothScrollToPosition(position);

        // Скрываем кнопку "вниз"
        binding.scrollToBottomFab.setVisibility(View.GONE);

        isAtBottom = true;
    }
    private void markMessagesAsRead() {
        if (chatId == null || currentUserId == null) return;

        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages");

        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    String ownerId = msgSnapshot.child("ownerId").getValue(String.class);
                    Boolean isRead = msgSnapshot.child("read").getValue(Boolean.class);

                    // Отмечаем только чужие непрочитанные сообщения
                    if (ownerId != null && !ownerId.equals(currentUserId)
                            && (isRead == null || !isRead)) {
                        msgSnapshot.getRef().child("read").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showChatSettingsSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_chat_settings, null);

        LinearLayout actionToggleNotifications = sheetView.findViewById(R.id.action_toggle_notifications);
        LinearLayout actionShareContact = sheetView.findViewById(R.id.action_share_contact);
        LinearLayout actionOpenProfile = sheetView.findViewById(R.id.action_open_profile);

        androidx.appcompat.widget.SwitchCompat notificationSwitch = sheetView.findViewById(R.id.notification_switch);
        TextView notificationSubtitle = sheetView.findViewById(R.id.notification_subtitle_tv);
        ImageView notificationIcon = sheetView.findViewById(R.id.notification_icon);

        // Загружаем текущее состояние уведомлений
        loadNotificationStatus(notificationSwitch, notificationSubtitle, notificationIcon);

        // Переключатель уведомлений
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleChatNotifications(isChecked);
            notificationSubtitle.setText(isChecked ? "Включены" : "Отключены");
            notificationIcon.setImageResource(
                    isChecked ? R.drawable.ic_notifications : R.drawable.ic_notifications_off
            );
        });

        actionToggleNotifications.setOnClickListener(v -> {
            notificationSwitch.setChecked(!notificationSwitch.isChecked());
        });

        // Поделиться контактом
        actionShareContact.setOnClickListener(v -> {
            bottomSheet.dismiss();
            shareContact();
        });

        // Открыть профиль
        actionOpenProfile.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (receiverId != null) {
                Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", receiverId);
                startActivity(intent);
            }
        });

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void toggleChatNotifications(boolean enabled) {
        if (chatId == null || currentUserId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("mutedBy")
                .child(currentUserId)
                .setValue(!enabled)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка изменения настроек", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadNotificationStatus(androidx.appcompat.widget.SwitchCompat switchCompat,
                                        TextView subtitle, ImageView icon) {
        if (chatId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("mutedBy")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isMuted = snapshot.getValue(Boolean.class);
                        boolean notificationsEnabled = isMuted == null || !isMuted;

                        switchCompat.setChecked(notificationsEnabled);
                        subtitle.setText(notificationsEnabled ? "Включены" : "Отключены");
                        icon.setImageResource(
                                notificationsEnabled ? R.drawable.ic_notifications : R.drawable.ic_notifications_off
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void shareContact() {
        if (receiverId == null) return;

        // Показываем диалог выбора чата
        Intent intent = new Intent(this, SelectChatActivity.class);
        intent.putExtra("shareContactUserId", receiverId);
        intent.putExtra("shareContactUsername", binding.chatUsernameTv.getText().toString());
        intent.putExtra("sourceChatId", chatId);
        startActivity(intent);
    }

    public void sendContactMessage(String contactUserId, String contactUsername) {
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HashMap<String, Object> msg = new HashMap<>();
        msg.put("text", "");
        msg.put("ownerId", currentUserId);
        msg.put("date", dateFormat.format(new Date()));
        msg.put("timestamp", now);
        msg.put("contactUserId", contactUserId);
        msg.put("contactUsername", contactUsername);
        msg.put("read", false);

        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .push();

        msgRef.setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    updateLastMessage("👤 Контакт: " + contactUsername, now);
                    incrementUnreadCount();
                    scrollToBottom();
                    Toast.makeText(this, "Контакт отправлен", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupChatMenu() {
        binding.menuBtn.setOnClickListener(v -> showChatSettingsSheet());

        binding.closeReplyBtn.setOnClickListener(v -> cancelReply());
    }
}