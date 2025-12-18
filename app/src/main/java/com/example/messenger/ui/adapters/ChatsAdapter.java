package com.example.messenger.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.ui.activities.ChatActivity;
import com.example.messenger.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private List<Map<String, Object>> chats;

    public ChatsAdapter(List<Map<String, Object>> chats) {
        this.chats = chats;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item_rv, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> chatData = chats.get(position);

        String username = (String) chatData.get("username");
        String chatId = (String) chatData.get("chatId");
        String otherUid = (String) chatData.get("otherUid");
        long lastMessageTime = (long) chatData.get("lastMessageTime");
        Boolean isOnline = (Boolean) chatData.get("isOnline");
        Long lastSeen = (Long) chatData.get("lastSeen");
        int unreadCount = chatData.get("unreadCount") != null ? (int) chatData.get("unreadCount") : 0;

        holder.usernameTv.setText(username != null ? username : "Loading...");
        updateUserStatus(holder, isOnline, lastSeen);
        loadProfileImage(holder, otherUid);

        holder.lastMessageTimeTv.setText(lastMessageTime > 0 ? formatTime(lastMessageTime) : "");

        if (unreadCount > 0) {
            holder.unreadCountTv.setText(String.valueOf(unreadCount));
            holder.unreadCountTv.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountTv.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (chatId != null) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("chatId", chatId);
                v.getContext().startActivity(intent);
            }
        });
    }

    private void updateUserStatus(ChatViewHolder holder, Boolean isOnline, Long lastSeen) {
        if (holder.statusTv == null) return;
        if (isOnline != null && isOnline) {
            holder.statusTv.setText("Online");
            holder.statusTv.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_green_dark));
            holder.statusTv.setVisibility(View.VISIBLE);
        } else if (lastSeen != null && lastSeen > 0) {
            holder.statusTv.setText("Was " + getTimeAgo(lastSeen));
            holder.statusTv.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.darker_gray));
            holder.statusTv.setVisibility(View.VISIBLE);
        } else {
            holder.statusTv.setVisibility(View.GONE);
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "just now";
        else if (minutes < 60) return minutes + " minute ago";
        else if (hours < 24) return hours + " hours ago";
        else if (days < 7) return days + " day ago";
        else return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private void loadProfileImage(ChatViewHolder holder, String uid) {
        if (uid == null) {
            holder.profileImageView.setImageResource(R.drawable.baseline_person_24);
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("profileImageUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String imageUrl = snapshot.getValue(String.class);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(holder.profileImageView);
                        } else holder.profileImageView.setImageResource(R.drawable.baseline_person_24);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.profileImageView.setImageResource(R.drawable.baseline_person_24);
                    }
                });
    }

    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 24 * 60 * 60 * 1000)
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        if (diff < 2 * 24 * 60 * 60 * 1000) return "Yesterday";
        return new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView usernameTv;
        TextView statusTv;
        TextView lastMessageTimeTv;
        TextView unreadCountTv;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.chat_profile_iv);
            usernameTv = itemView.findViewById(R.id.chat_username_tv);
            statusTv = itemView.findViewById(R.id.chat_status_tv);
            lastMessageTimeTv = itemView.findViewById(R.id.last_message_time_tv);
            unreadCountTv = itemView.findViewById(R.id.unread_count_tv);
        }
    }
}
