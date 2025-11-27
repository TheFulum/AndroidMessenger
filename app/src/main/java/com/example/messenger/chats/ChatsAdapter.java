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

    private ArrayList<Map<String, Object>> chats;

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

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            holder.usernameTv.setText("Not authenticated");
            return;
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String otherUid = chat.getUser1().equals(myUid)
                ? chat.getUser2()
                : chat.getUser1();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(otherUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String username = snap.child("username").getValue(String.class);
                        if (username != null) {
                            holder.usernameTv.setText(username);
                        } else {
                            holder.usernameTv.setText("Unknown user");
                        }

                    } else {
                        holder.usernameTv.setText("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.usernameTv.setText("Error loading user");
                });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("chatId", chatId);
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