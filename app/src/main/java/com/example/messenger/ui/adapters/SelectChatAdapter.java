package com.example.messenger.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

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
        String otherUid = (String) chatData.get("otherUid");

        holder.usernameTv.setText(username != null ? username : "Unknown");

        // Загружаем фото профиля
        loadProfileImage(holder, otherUid);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && chatId != null) {
                listener.onChatSelected(chatId);
            }
        });
    }

    /**
     * Загружает фото профиля пользователя
     */
    private void loadProfileImage(ViewHolder holder, String uid) {
        if (uid == null) {
            holder.profileIv.setImageResource(R.drawable.baseline_person_24);
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
                                    .into(holder.profileIv);
                        } else {
                            holder.profileIv.setImageResource(R.drawable.baseline_person_24);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.profileIv.setImageResource(R.drawable.baseline_person_24);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTv;
        CircleImageView profileIv;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.username_tv);
            profileIv = itemView.findViewById(R.id.profile_iv);
        }
    }
}