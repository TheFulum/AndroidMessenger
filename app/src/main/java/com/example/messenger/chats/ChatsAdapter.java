package com.example.messenger.chats;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.ChatActivity;
import com.example.messenger.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private ArrayList<Map<String, Object>> chats; // Теперь принимаем Map

    public ChatsAdapter(ArrayList<Map<String, Object>> chats) {
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
        Map<String, Object> chatData = chats.get(position);
        String chatId = (String) chatData.get("chatId");
        Chat chat = (Chat) chatData.get("chat");

        Log.d("ChatsAdapter", "Binding chat at position " + position +
                ", chatId: " + chatId +
                ", user1: " + chat.getUser1() +
                ", user2: " + chat.getUser2());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            holder.usernameTv.setText("Not authenticated");
            return;
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String otherUid = chat.getUser1().equals(myUid)
                ? chat.getUser2()
                : chat.getUser1();

        Log.d("ChatsAdapter", "My UID: " + myUid + ", Other UID: " + otherUid);

        // Загружаем данные другого пользователя
        FirebaseDatabase.getInstance().getReference("Users")
                .child(otherUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String username = snap.child("username").getValue(String.class);
                        if (username != null) {
                            holder.usernameTv.setText(username);
                            Log.d("ChatsAdapter", "Loaded username: " + username);
                        } else {
                            holder.usernameTv.setText("Unknown user");
                        }

                        // Загружаем аватарку если есть
                        String profileImage = snap.child("profileImage").getValue(String.class);
                        if (profileImage != null && !profileImage.isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(profileImage)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .into(holder.profileIv);
                        } else {
                            holder.profileIv.setImageResource(R.drawable.baseline_person_24);
                        }
                    } else {
                        holder.usernameTv.setText("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.usernameTv.setText("Error loading user");
                    Log.e("ChatsAdapter", "Error loading user data: " + e.getMessage());
                });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("chatId", chatId); // Передаем правильный chatId
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileIv;
        TextView usernameTv;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profile_iv);
            usernameTv = itemView.findViewById(R.id.username_tv);
        }
    }
}