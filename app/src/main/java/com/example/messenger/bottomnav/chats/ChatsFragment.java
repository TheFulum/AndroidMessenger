package com.example.messenger.bottomnav.chats;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.R;
import com.example.messenger.chats.Chat;
import com.example.messenger.chats.ChatsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ChatsFragment extends Fragment {

    private androidx.recyclerview.widget.RecyclerView chatsRv;
    private EditText searchEt;

    private final ArrayList<Map<String, Object>> chats = new ArrayList<>();
    private final ArrayList<Map<String, Object>> filteredChats = new ArrayList<>();

    private ChatsAdapter chatsAdapter;
    private String myUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        chatsRv = view.findViewById(R.id.chats_rv);
        searchEt = view.findViewById(R.id.search_et);

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatsRv.setLayoutManager(new LinearLayoutManager(getContext()));

        chatsRv.addItemDecoration(
                new androidx.recyclerview.widget.DividerItemDecoration(
                        getContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                )
        );

        chatsAdapter = new ChatsAdapter(filteredChats);
        chatsRv.setAdapter(chatsAdapter);

        loadChats();
        setupSearch();

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearch() {
        updateClearIcon(false);

        // КРИТИЧНО: TextWatcher срабатывает при КАЖДОМ изменении текста!
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                // Не используем
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                updateClearIcon(s.length() > 0);
                // МГНОВЕННО фильтруем при КАЖДОМ символе!
                applyFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не используем
            }
        });

        searchEt.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isClearIconClicked(searchEt, event)) {
                    searchEt.setText("");
                    hideKeyboard();
                    return true;
                }
            }
            return false;
        });

        searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void updateClearIcon(boolean show) {
        searchEt.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_search,
                0,
                show ? R.drawable.ic_clear : 0,
                0
        );
    }

    private boolean isClearIconClicked(EditText editText, MotionEvent event) {
        if (editText.getCompoundDrawables()[2] == null) {
            return false;
        }

        float touchX = event.getX();
        int clearIconStart = editText.getWidth() - editText.getPaddingEnd() -
                editText.getCompoundDrawables()[2].getIntrinsicWidth();

        return touchX >= clearIconStart;
    }

    private void hideKeyboard() {
        if (getContext() == null || searchEt == null) return;

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEt.getWindowToken(), 0);
        }
    }

    private void loadChats() {
        FirebaseDatabase.getInstance().getReference("Chats")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        chats.clear();

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {

                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat == null) continue;

                            if (!myUid.equals(chat.getUser1()) && !myUid.equals(chat.getUser2()))
                                continue;

                            String otherUid = myUid.equals(chat.getUser1()) ? chat.getUser2() : chat.getUser1();
                            String chatId = chatSnap.getKey();

                            Long lastMessageTime = chatSnap.child("lastMessageTime").getValue(Long.class);

                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("chatId", chatId);
                            chatData.put("otherUid", otherUid);
                            chatData.put("chat", chat);
                            chatData.put("lastMessageTime", lastMessageTime != null ? lastMessageTime : 0L);
                            chatData.put("username", "Loading...");

                            chats.add(chatData);
                        }

                        // Сортируем и показываем сразу
                        sortChats();
                        applyFilter(searchEt.getText().toString());

                        // Загружаем username асинхронно
                        loadUsernames();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUsernames() {
        if (chats.isEmpty()) return;

        FirebaseDatabase db = FirebaseDatabase.getInstance();

        for (Map<String, Object> chatData : chats) {
            String otherUid = (String) chatData.get("otherUid");

            db.getReference("Users").child(otherUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            String username = snap.child("username").getValue(String.class);
                            chatData.put("username", username != null ? username : "Unknown");

                            // МГНОВЕННО обновляем после КАЖДОЙ загрузки!
                            sortChats();
                            applyFilter(searchEt.getText().toString());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            chatData.put("username", "Unknown");
                            sortChats();
                            applyFilter(searchEt.getText().toString());
                        }
                    });
        }
    }

    private void sortChats() {
        chats.sort((a, b) -> {
            long t1 = (long) a.get("lastMessageTime");
            long t2 = (long) b.get("lastMessageTime");
            return Long.compare(t2, t1);
        });
    }

    /**
     * КРИТИЧНО: Фильтрует и СРАЗУ обновляет список
     * Регистронезависимый поиск
     */
    private void applyFilter(String query) {
        // Регистронезависимый поиск
        String text = query.toLowerCase(Locale.ROOT).trim();

        filteredChats.clear();

        if (text.isEmpty()) {
            filteredChats.addAll(chats);
        } else {
            for (Map<String, Object> chat : chats) {
                String username = (String) chat.get("username");
                // Приводим username к нижнему регистру для сравнения
                if (username != null && username.toLowerCase(Locale.ROOT).contains(text)) {
                    filteredChats.add(chat);
                }
            }
        }

        // МГНОВЕННОЕ обновление UI!
        chatsAdapter.notifyDataSetChanged();
    }
}