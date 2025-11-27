package com.example.messenger.bottomnav.chats;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.R;
import com.example.messenger.chats.Chat;
import com.example.messenger.chats.ChatsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatsFragment extends Fragment {

    private RecyclerView chatsRv;
    private ArrayList<Map<String, Object>> chats;
    private ChatsAdapter chatsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        chatsRv = view.findViewById(R.id.chats_rv);
        chatsRv.setLayoutManager(new LinearLayoutManager(getContext()));

        chats = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(chats);
        chatsRv.setAdapter(chatsAdapter);

        loadChats();

        return view;
    }

    private void loadChats() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("ChatsFragment", "User not authenticated");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("ChatsFragment", "Loading chats for user: " + uid);

        chats.clear();

        FirebaseDatabase.getInstance().getReference("Chats")
                .orderByChild("user1")
                .equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("ChatsFragment", "Query 1 - Found " + snapshot.getChildrenCount() + " chats where user1 = " + uid);

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {
                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat != null) {
                                Map<String, Object> chatData = new HashMap<>();
                                chatData.put("chatId", chatSnap.getKey());
                                chatData.put("chat", chat);

                                if (!containsChat(chatSnap.getKey())) {
                                    chats.add(chatData);
                                    Log.d("ChatsFragment", "Added chat from user1: " + chatSnap.getKey());
                                }
                            }
                        }
                        chatsAdapter.notifyDataSetChanged();
                        Log.d("ChatsFragment", "Total chats after user1 query: " + chats.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatsFragment", "Error loading chats 1: " + error.getMessage());
                    }
                });

        FirebaseDatabase.getInstance().getReference("Chats")
                .orderByChild("user2")
                .equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("ChatsFragment", "Query 2 - Found " + snapshot.getChildrenCount() + " chats where user2 = " + uid);

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {
                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat != null) {
                                Map<String, Object> chatData = new HashMap<>();
                                chatData.put("chatId", chatSnap.getKey());
                                chatData.put("chat", chat);

                                if (!containsChat(chatSnap.getKey())) {
                                    chats.add(chatData);
                                    Log.d("ChatsFragment", "Added chat from user2: " + chatSnap.getKey());
                                }
                            }
                        }
                        chatsAdapter.notifyDataSetChanged();
                        Log.d("ChatsFragment", "Total chats after both queries: " + chats.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatsFragment", "Error loading chats 2: " + error.getMessage());
                    }
                });
    }

    private boolean containsChat(String chatId) {
        for (Map<String, Object> chatData : chats) {
            String existingChatId = (String) chatData.get("chatId");
            if (existingChatId != null && existingChatId.equals(chatId)) {
                return true;
            }
        }
        return false;
    }
}