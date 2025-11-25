package com.example.messenger.chats;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.ChatActivity;
import com.example.messenger.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private ArrayList<Chat> chats;

    public ChatsAdapter(ArrayList<Chat> chats) {
        this.chats = chats;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.person_item_rv, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);

        Log.d("ChatsAdapter", "Binding chat at position " + position +
                ", chatId: " + chat.getChatId() +
                ", user1: " + chat.getUser1() +
                ", user2: " + chat.getUser2());

        // Проверяем авторизацию
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            holder.usernameTv.setText("Not authenticated");
            return;
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String otherUid = chat.getUser1().equals(myUid)
                ? chat.getUser2()
                : chat.getUser1();

        Log.d("ChatsAdapter", "My UID: " + myUid + ", Other UID: " + otherUid);

        // Загружаем username другого пользователя
        FirebaseDatabase.getInstance().getReference("Users")
                .child(otherUid)
                .child("username")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String username = snap.getValue(String.class);
                        holder.usernameTv.setText(username);
                        Log.d("ChatsAdapter", "Loaded username: " + username);
                    } else {
                        holder.usernameTv.setText("Unknown user");
                        Log.d("ChatsAdapter", "Username not found for UID: " + otherUid);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.usernameTv.setText("Error loading user");
                    Log.e("ChatsAdapter", "Error loading username: " + e.getMessage());
                });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("chatId", chat.getChatId()); // Исправлено: getChatId() вместо getChat_id()
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTv;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.username_tv);
        }
    }
}