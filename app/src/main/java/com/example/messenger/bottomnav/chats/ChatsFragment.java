package com.example.messenger.bottomnav.chats;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.R;
import com.example.messenger.chats.Chat;
import com.example.messenger.chats.ChatsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        chatsRv = view.findViewById(R.id.chats_rv);
        searchEt = view.findViewById(R.id.search_et);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Сессия ещё не восстановилась → Просто не грузим чаты
            return view;
        }

        myUid = user.getUid();

        chatsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        chatsRv.addItemDecoration(new DividerItemDecoration(
                getContext(), DividerItemDecoration.VERTICAL));

        chatsAdapter = new ChatsAdapter(filteredChats);
        chatsRv.setAdapter(chatsAdapter);

        loadChats();
        setupSearch();

        return view;
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

                            String user1 = chat.getUser1();
                            String user2 = chat.getUser2();

                            if (!myUid.equals(user1) && !myUid.equals(user2)) continue;

                            String otherUid = myUid.equals(user1) ? user2 : user1;
                            String chatId = chatSnap.getKey();

                            Map<String, Object> map = new HashMap<>();
                            map.put("chatId", chatId);
                            map.put("otherUid", otherUid);
                            map.put("chat", chat);

                            chats.add(map);
                        }

                        loadUsernamesAndLastMessages();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUsernamesAndLastMessages() {
        int total = chats.size();
        if (total == 0) {
            filteredChats.clear();
            chatsAdapter.notifyDataSetChanged();
            return;
        }

        final int[] loaded = {0};

        for (Map<String, Object> chatData : chats) {
            String otherUid = (String) chatData.get("otherUid");
            String chatId = (String) chatData.get("chatId");

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(otherUid)
                    .child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            String username = snap.getValue(String.class);
                            chatData.put("username", username != null ? username : "Пользователь");

                            // Загружаем последнее сообщение
                            FirebaseDatabase.getInstance()
                                    .getReference("Chats")
                                    .child(chatId)
                                    .child("messages")
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot msgSnap) {

                                            String lastText = "Начните общение";
                                            String lastDate = "";
                                            long timestamp = 0;

                                            if (msgSnap.exists()) {
                                                DataSnapshot lastMsg = msgSnap.getChildren().iterator().next();
                                                lastText = lastMsg.child("text").getValue(String.class);
                                                lastDate = lastMsg.child("date").getValue(String.class);

                                                if (lastDate != null) {
                                                    try {
                                                        timestamp = sdf.parse(lastDate).getTime();
                                                    } catch (Exception ignored) {}
                                                }
                                            }

                                            chatData.put("lastMessage", lastText);
                                            chatData.put("lastDate", lastDate);
                                            chatData.put("timestamp", timestamp);

                                            loaded[0]++;
                                            if (loaded[0] == total) {
                                                sortAndRefresh();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            loaded[0]++;
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                        }
                    });
        }
    }

    private void sortAndRefresh() {
        chats.sort((a, b) -> Long.compare((long) b.get("timestamp"), (long) a.get("timestamp")));
        applyFilter();
    }

    private void setupSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String q = searchEt.getText().toString().toLowerCase();

        filteredChats.clear();

        for (Map<String, Object> chat : chats) {
            String username = ((String) chat.get("username")).toLowerCase();
            if (username.contains(q)) {
                filteredChats.add(chat);
            }
        }

        chatsAdapter.notifyDataSetChanged();
    }
}
