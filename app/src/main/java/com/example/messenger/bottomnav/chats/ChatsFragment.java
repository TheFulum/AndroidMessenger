package com.example.messenger.bottomnav.chats;

import android.os.Bundle;
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
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private RecyclerView chatsRv;
    private ArrayList<Chat> chats;
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
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Chats")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    chats.clear();

                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        Chat chat = snapshot.getValue(Chat.class);

                        if (chat == null) continue;

                        chat.setChat_id(snapshot.getKey());

                        if (chat.getUser1().equals(currentUid) ||
                                chat.getUser2().equals(currentUid)) {

                            chats.add(chat);
                        }
                    }

                    chatsAdapter.notifyDataSetChanged();
                });
    }
}
