package com.example.messenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.R;

import java.util.List;
import java.util.Map;

public class SelectChatAdapter extends RecyclerView.Adapter<SelectChatAdapter.ViewHolder> {

    private List<Map<String, Object>> chats;
    private OnChatSelectedListener listener;

    public interface OnChatSelectedListener {
        void onChatSelected(String chatId);
    }

    public SelectChatAdapter(List<Map<String, Object>> chats, OnChatSelectedListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.person_item_rv, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> chatData = chats.get(position);
        String username = (String) chatData.get("username");
        String chatId = (String) chatData.get("chatId");

        holder.usernameTv.setText(username != null ? username : "Unknown");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && chatId != null) {
                listener.onChatSelected(chatId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTv;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.username_tv);
        }
    }
}