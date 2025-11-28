package com.example.messenger.chats;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.ChatActivity;
import com.example.messenger.R;

import java.util.ArrayList;
import java.util.Map;

public class ChatsAdapter extends RecyclerView.Adapter<ChatViewHolder> {

    private ArrayList<Map<String, Object>> chats;

    public ChatsAdapter(ArrayList<Map<String, Object>> chats) {
        this.chats = chats;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_item_rv, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int pos) {
        Map<String, Object> data = chats.get(pos);

        String username = (String) data.get("username");
        String chatId = (String) data.get("chatId");

        holder.chat_name_tv.setText(username != null ? username : "Unknown");

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(holder.itemView.getContext(), ChatActivity.class);
            i.putExtra("chatId", chatId);
            holder.itemView.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }
}
