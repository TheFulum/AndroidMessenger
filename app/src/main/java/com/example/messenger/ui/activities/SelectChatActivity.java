package com.example.messenger.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.R;
import com.example.messenger.ui.adapters.SelectChatAdapter;
import com.example.messenger.data.models.Chat;
import com.example.messenger.databinding.ActivitySelectChatBinding;
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

public class SelectChatActivity extends AppCompatActivity {

    private ActivitySelectChatBinding binding;
    private String messageText;
    private String currentUserId;
    private String currentUsername;
    private String sourceChatId;

    private String fileUrl;
    private String fileType;
    private String fileName;
    private long fileSize;
    private long voiceDuration;
    private boolean hasFile = false;

    private List<Map<String, Object>> allChats = new ArrayList<>();
    private List<Map<String, Object>> filteredChats = new ArrayList<>();
    private SelectChatAdapter adapter;
    private ValueEventListener chatsListener;
    private DatabaseReference chatsRef;
    private boolean isForwarding = false;
    private String shareContactUserId;
    private String shareContactUsername;
    private boolean isSharingContact = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        messageText = getIntent().getStringExtra("messageText");
        sourceChatId = getIntent().getStringExtra("sourceChatId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        fileUrl = getIntent().getStringExtra("fileUrl");
        fileType = getIntent().getStringExtra("fileType");
        fileName = getIntent().getStringExtra("fileName");
        fileSize = getIntent().getLongExtra("fileSize", 0);
        voiceDuration = getIntent().getLongExtra("voiceDuration", 0);
        hasFile = fileUrl != null && !fileUrl.isEmpty();

        shareContactUserId = getIntent().getStringExtra("shareContactUserId");
        shareContactUsername = getIntent().getStringExtra("shareContactUsername");
        isSharingContact = shareContactUserId != null && !shareContactUserId.isEmpty();

        if (currentUserId == null) {
            Toast.makeText(this, "Forwarding error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupSearch();
        setupRecyclerView();
        loadCurrentUsername();
        loadChats();
    }

    private void setupToolbar() {
        binding.backBtn.setOnClickListener(v -> {
            if (!isForwarding) finish();
        });

        String title = "Forwarding ";

        if (isSharingContact) {
            title = "Share contact";
        } else if (hasFile) {
            if ("image".equals(fileType)) {
                title += "📷 photo";
            } else if ("voice".equals(fileType)) {
                title += "🎤 voice message";
            } else {
                title += "📄 document";
            }
        } else {
            title += "message";
        }

        binding.titleTv.setText(title);
    }

    private void loadCurrentUsername() {
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId)
                .child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUsername = snapshot.getValue(String.class);
                        if (currentUsername == null) {
                            currentUsername = "Unknown";
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentUsername = "Unknown";
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearch() {
        updateClearIcon(false);

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearIcon(s.length() > 0);
                filterChats(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.searchEt.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isClearIconClicked(binding.searchEt, event)) {
                    binding.searchEt.setText("");
                    hideKeyboard();
                    return true;
                }
            }
            return false;
        });

        binding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void updateClearIcon(boolean show) {
        binding.searchEt.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_search, 0, show ? R.drawable.ic_clear : 0, 0
        );
    }

    private boolean isClearIconClicked(android.widget.EditText editText, MotionEvent event) {
        if (editText.getCompoundDrawables()[2] == null) return false;

        float touchX = event.getX();
        int clearIconStart = editText.getWidth() - editText.getPaddingEnd() -
                editText.getCompoundDrawables()[2].getIntrinsicWidth();

        return touchX >= clearIconStart;
    }

    private void filterChats(String query) {
        String searchQuery = query.toLowerCase(Locale.ROOT).trim();
        filteredChats.clear();

        if (searchQuery.isEmpty()) {
            filteredChats.addAll(allChats);
        } else {
            for (Map<String, Object> chat : allChats) {
                String username = (String) chat.get("username");
                if (username != null && username.toLowerCase().contains(searchQuery)) {
                    filteredChats.add(chat);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void hideKeyboard() {
        if (getApplicationContext() == null || binding.searchEt == null) return;

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchEt.getWindowToken(), 0);
        }
    }

    private void setupRecyclerView() {
        binding.chatsRv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SelectChatAdapter(filteredChats, this::forwardMessageToChat);
        binding.chatsRv.setAdapter(adapter);
    }

    private void loadChats() {
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allChats.clear();

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chat chat = chatSnap.getValue(Chat.class);
                    if (chat == null) continue;

                    if (!currentUserId.equals(chat.getUser1()) &&
                            !currentUserId.equals(chat.getUser2())) {
                        continue;
                    }

                    String otherUid = currentUserId.equals(chat.getUser1())
                            ? chat.getUser2()
                            : chat.getUser1();
                    String chatId = chatSnap.getKey();

                    if (chatId != null && chatId.equals(sourceChatId)) continue;

                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("chatId", chatId);
                    chatData.put("otherUid", otherUid);
                    chatData.put("username", "Loading...");

                    allChats.add(chatData);
                }

                if (allChats.isEmpty()) {
                    Toast.makeText(SelectChatActivity.this, "No available chats", Toast.LENGTH_SHORT).show();
                }

                filterChats(binding.searchEt.getText().toString());
                loadUsernames();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectChatActivity.this, "Download error", Toast.LENGTH_SHORT).show();
            }
        };

        chatsRef.addValueEventListener(chatsListener);
    }

    private void loadUsernames() {
        if (allChats.isEmpty()) return;

        for (Map<String, Object> chatData : allChats) {
            String otherUid = (String) chatData.get("otherUid");
            if (otherUid == null) continue;

            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(otherUid)
                    .child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            String username = snap.getValue(String.class);
                            chatData.put("username", username != null ? username : "Unknown");
                            filterChats(binding.searchEt.getText().toString());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            chatData.put("username", "Unknown");
                            filterChats(binding.searchEt.getText().toString());
                        }
                    });
        }
    }

    private void forwardMessageToChat(String targetChatId) {
        if (isForwarding || targetChatId == null) return;

        isForwarding = true;
        binding.backBtn.setEnabled(false);

        String loadingMessage = isSharingContact ? "Sharing contact..." : "Forwarding...";
        Toast.makeText(this, loadingMessage, Toast.LENGTH_SHORT).show();

        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HashMap<String, Object> msg = new HashMap<>();

        if (isSharingContact) {
            msg.put("text", "");
            msg.put("ownerId", currentUserId);
            msg.put("date", dateFormat.format(new Date()));
            msg.put("timestamp", now);
            msg.put("contactUserId", shareContactUserId);
            msg.put("contactUsername", shareContactUsername);
            msg.put("read", false);
        } else {
            String forwardedText;
            if (hasFile) {
                if (messageText != null && !messageText.isEmpty()) {
                    forwardedText = messageText;
                } else {
                    forwardedText = "";
                }
            } else {
                forwardedText = "📩 Forwarded from " + currentUsername + ":\n" +
                        (messageText != null ? messageText : "");
            }

            msg.put("text", forwardedText);
            msg.put("ownerId", currentUserId);
            msg.put("date", dateFormat.format(new Date()));
            msg.put("timestamp", now);
            msg.put("isForwarded", true);
            msg.put("forwardedFrom", currentUsername);

            if (hasFile) {
                msg.put("fileUrl", fileUrl);
                msg.put("fileType", fileType);
                msg.put("fileName", fileName);
                msg.put("fileSize", fileSize);

                if ("voice".equals(fileType)) {
                    msg.put("voiceDuration", voiceDuration);
                }
            }
        }

        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(targetChatId)
                .child("messages")
                .push();

        messageRef.setValue(msg)
                .addOnSuccessListener(aVoid -> updateLastMessage(targetChatId, now))
                .addOnFailureListener(e -> {
                    isForwarding = false;
                    binding.backBtn.setEnabled(true);
                    String errorMsg = isSharingContact ? "Sharing error" : "Forwarding error";
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String targetChatId, long timestamp) {
        HashMap<String, Object> update = new HashMap<>();
        update.put("lastMessageTime", timestamp);

        String preview;

        if (isSharingContact) {
            preview = "👤 Contact: " + shareContactUsername;
        } else if (hasFile) {
            if ("image".equals(fileType)) {
                preview = "📩 Forwarded: 📷 Photo";
            } else if ("voice".equals(fileType)) {
                preview = "📩 Forwarded: 🎤 Voice message";
            } else {
                preview = "📩 Forwarded: 📄 " + fileName;
            }
        } else {
            preview = "📩 Forwarded from " + currentUsername;
        }

        update.put("lastMessagePreview", preview);

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(targetChatId)
                .updateChildren(update)
                .addOnSuccessListener(aVoid -> {
                    String successMsg = isSharingContact ?
                            "Contact shared successfully" : "The message has been forwarded";
                    Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    String successMsg = isSharingContact ?
                            "Contact shared successfully" : "The message has been forwarded";
                    Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @NonNull
    private HashMap<String, Object> getStringObjectHashMap(long timestamp) {
        HashMap<String, Object> update = new HashMap<>();
        update.put("lastMessageTime", timestamp);

        String preview;
        if (hasFile) {
            if ("image".equals(fileType)) {
                preview = "📩 Forwarded: 📷 Photo";
            } else if ("voice".equals(fileType)) {
                preview = "📩 Forwarded: 🎤 Voice message";
            } else {
                preview = "📩 Forwarded: 📄 " + fileName;
            }
        } else {
            preview = "📩 Forwarded from " + currentUsername;
        }

        update.put("lastMessagePreview", preview);
        return update;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }

        binding = null;
    }
}