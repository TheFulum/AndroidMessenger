package com.example.messenger.chats;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.ChatActivity;
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
        Chat chat = (Chat) chatData.get("chat");
        long lastMessageTime = (long) chatData.get("lastMessageTime");

        // Получаем статус онлайн
        Boolean isOnline = (Boolean) chatData.get("isOnline");
        Long lastSeen = (Long) chatData.get("lastSeen");

        // Устанавливаем username
        holder.usernameTv.setText(username != null ? username : "Loading...");

        // Обновляем статус пользователя
        updateUserStatus(holder, isOnline, lastSeen);

        // Загружаем фото профиля собеседника
        loadProfileImage(holder, otherUid);

        // Форматируем время последнего сообщения
        if (lastMessageTime > 0) {
            holder.lastMessageTimeTv.setText(formatTime(lastMessageTime));
        } else {
            holder.lastMessageTimeTv.setText("");
        }

        // Клик по чату
        holder.itemView.setOnClickListener(v -> {
            if (chatId != null) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("chatId", chatId);
                v.getContext().startActivity(intent);
            }
        });
    }

    /**
     * Обновляет статус пользователя (В сети / Был в сети)
     */
    private void updateUserStatus(ChatViewHolder holder, Boolean isOnline, Long lastSeen) {
        if (holder.statusTv == null) return; // Если статус TextView отсутствует в layout

        if (isOnline != null && isOnline) {
            holder.statusTv.setText("В сети");
            holder.statusTv.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_green_dark));
            holder.statusTv.setVisibility(View.VISIBLE);
        } else if (lastSeen != null && lastSeen > 0) {
            String timeAgo = getTimeAgo(lastSeen);
            holder.statusTv.setText("Был(а) " + timeAgo);
            holder.statusTv.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.darker_gray));
            holder.statusTv.setVisibility(View.VISIBLE);
        } else {
            holder.statusTv.setVisibility(View.GONE);
        }
    }

    /**
     * Форматирует timestamp в "только что", "5 мин назад" и т.д.
     */
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "только что";
        } else if (minutes < 60) {
            return minutes + " мин. назад";
        } else if (hours < 24) {
            return hours + " ч. назад";
        } else if (days < 7) {
            return days + " д. назад";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    /**
     * Загружает фото профиля пользователя из Firebase
     */
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
                        } else {
                            holder.profileImageView.setImageResource(R.drawable.baseline_person_24);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.profileImageView.setImageResource(R.drawable.baseline_person_24);
                    }
                });
    }

    /**
     * Форматирует timestamp в читаемое время
     */
    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // Сегодня - показываем только время
        if (diff < 24 * 60 * 60 * 1000) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(new Date(timestamp));
        }

        // Вчера
        if (diff < 2 * 24 * 60 * 60 * 1000) {
            return "Вчера";
        }

        // Больше - показываем дату
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView usernameTv;
        TextView statusTv; // ДОБАВИЛИ
        TextView lastMessagePreviewTv;
        TextView lastMessageTimeTv;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.chat_profile_iv);
            usernameTv = itemView.findViewById(R.id.chat_username_tv);
            statusTv = itemView.findViewById(R.id.chat_status_tv); // ДОБАВИЛИ
            lastMessageTimeTv = itemView.findViewById(R.id.last_message_time_tv);
        }
    }
}