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

public class ChatsFragment extends Fragment {

    private RecyclerView chatsRv;
    private ArrayList<Chat> chats;
    private ChatsAdapter chatsAdapter;
    private ValueEventListener chatsListener1, chatsListener2;

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
        Log.d("ChatsFragment", "Current user UID: " + uid);

        // Очищаем список
        chats.clear();

        // Первый слушатель для чатов где пользователь - user1
        chatsListener1 = FirebaseDatabase.getInstance().getReference("Chats")
                .orderByChild("user1")
                .equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("ChatsFragment", "Query 1 - Found " + snapshot.getChildrenCount() + " chats where user1 = " + uid);

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {
                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat != null) {
                                // Устанавливаем ID чата из ключа
                                chat.setChatId(chatSnap.getKey());
                                // Проверяем на дубликаты
                                if (!containsChat(chat.getChatId())) {
                                    chats.add(chat);
                                    Log.d("ChatsFragment", "Added chat from user1: " + chat.getChatId() +
                                            " user1: " + chat.getUser1() + " user2: " + chat.getUser2());
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

        // Второй слушатель для чатов где пользователь - user2
        chatsListener2 = FirebaseDatabase.getInstance().getReference("Chats")
                .orderByChild("user2")
                .equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("ChatsFragment", "Query 2 - Found " + snapshot.getChildrenCount() + " chats where user2 = " + uid);

                        for (DataSnapshot chatSnap : snapshot.getChildren()) {
                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat != null) {
                                // Устанавливаем ID чата из ключа
                                chat.setChatId(chatSnap.getKey());
                                // Проверяем на дубликаты
                                if (!containsChat(chat.getChatId())) {
                                    chats.add(chat);
                                    Log.d("ChatsFragment", "Added chat from user2: " + chat.getChatId() +
                                            " user1: " + chat.getUser1() + " user2: " + chat.getUser2());
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

    // Метод для проверки дубликатов чатов
    private boolean containsChat(String chatId) {
        for (Chat chat : chats) {
            if (chat.getChatId() != null && chat.getChatId().equals(chatId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Удаляем слушатели при уничтожении фрагмента
        if (chatsListener1 != null) {
            FirebaseDatabase.getInstance().getReference("Chats").removeEventListener(chatsListener1);
        }
        if (chatsListener2 != null) {
            FirebaseDatabase.getInstance().getReference("Chats").removeEventListener(chatsListener2);
        }
    }
}