package com.example.messenger.bottomnav.chats;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.R;
import com.example.messenger.chats.Chat;
import com.example.messenger.chats.ChatsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatsFragment extends Fragment {

    private androidx.recyclerview.widget.RecyclerView chatsRv;
    private android.widget.EditText searchEt;

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

        // ================= ДОБАВИЛ — ДЕКОРАТОР =================
        chatsRv.addItemDecoration(
                new androidx.recyclerview.widget.DividerItemDecoration(
                        getContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                )
        );
        // =======================================================

        chatsAdapter = new ChatsAdapter(filteredChats);
        chatsRv.setAdapter(chatsAdapter);

        loadChats();
        setupSearch();

        return view;
    }


    // ============================ ЗАГРУЗКА ЧАТОВ ============================
    private void loadChats() {
        FirebaseDatabase.getInstance().getReference("Chats")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        chats.clear();

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {

                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat == null) continue;

                            // Проверяем: участвует ли текущий пользователь
                            if (!myUid.equals(chat.getUser1()) && !myUid.equals(chat.getUser2()))
                                continue;

                            String otherUid = myUid.equals(chat.getUser1()) ? chat.getUser2() : chat.getUser1();

                            String chatId = chatSnap.getKey();

                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("chatId", chatId);
                            chatData.put("otherUid", otherUid);
                            chatData.put("chat", chat);

                            chats.add(chatData);
                        }

                        // Загружаем имена собеседников
                        loadUsernames();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ============================ ЗАГРУЗКА ИМЕН ============================
    private void loadUsernames() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        for (Map<String, Object> chatData : chats) {
            String otherUid = (String) chatData.get("otherUid");

            db.getReference("Users").child(otherUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            String username = snap.child("username").getValue(String.class);
                            chatData.put("username", username != null ? username : "Unknown");

                            sortChats();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    // ============================ СОРТИРОВКА ЧАТОВ ============================
    private void sortChats() {
        chats.sort((a, b) -> {
            long t1 = getLastMessageTime((String) a.get("chatId"));
            long t2 = getLastMessageTime((String) b.get("chatId"));
            return Long.compare(t2, t1);
        });

        applyFilter();
    }

    // Получаем время последнего сообщения чата
    private long getLastMessageTime(String chatId) {
        try {
            DataSnapshot snap = FirebaseDatabase.getInstance()
                    .getReference("Chats")
                    .child(chatId)
                    .child("messages")
                    .get().getResult();

            long last = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

            if (snap != null) {
                for (DataSnapshot m : snap.getChildren()) {
                    String date = m.child("date").getValue(String.class);
                    if (date != null) {
                        last = sdf.parse(date).getTime();
                    }
                }
            }

            return last;

        } catch (Exception e) {
            return 0;
        }
    }

    // ============================ ПОИСК ============================
    private void setupSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String text = searchEt.getText().toString().toLowerCase(Locale.ROOT);

        filteredChats.clear();

        for (Map<String, Object> chat : chats) {
            String username = (String) chat.get("username");
            if (username != null && username.toLowerCase().contains(text)) {
                filteredChats.add(chat);
            }
        }

        chatsAdapter.notifyDataSetChanged();
    }
}
