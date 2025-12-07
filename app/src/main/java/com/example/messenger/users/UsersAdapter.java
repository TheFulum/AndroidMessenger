package com.example.messenger.users;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.ChatActivity;
import com.example.messenger.R;
import com.example.messenger.chats.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UserViewHolder> {

    private ArrayList<User> users;

    public UsersAdapter(ArrayList<User> users){
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.person_item_rv, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        holder.username_tv.setText(user.username);

        // Загружаем фото профиля
        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.baseline_person_24);
        }

        holder.itemView.setOnClickListener(v -> {
            createOrOpenChat(user, holder);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private void createOrOpenChat(User otherUser, UserViewHolder holder) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String otherUid = otherUser.uid;

        String chatId1 = myUid + "_" + otherUid;
        String chatId2 = otherUid + "_" + myUid;

        FirebaseDatabase.getInstance().getReference()
                .child("Chats")
                .child(chatId1)
                .get()
                .addOnSuccessListener(snapshot1 -> {
                    if (snapshot1.exists()) {
                        openChat(chatId1, holder);
                    } else {
                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(chatId2)
                                .get()
                                .addOnSuccessListener(snapshot2 -> {
                                    if (snapshot2.exists()) {
                                        openChat(chatId2, holder);
                                    } else {
                                        createChat(myUid, otherUid, chatId1, holder);
                                    }
                                });
                    }
                });
    }

    private void createChat(String myUid, String otherUid, String chatId, UserViewHolder holder) {
        Chat chat = new Chat(myUid, otherUid);

        FirebaseDatabase.getInstance().getReference()
                .child("Chats")
                .child(chatId)
                .setValue(chat)
                .addOnSuccessListener(unused -> openChat(chatId, holder));
    }

    private void openChat(String chatId, UserViewHolder holder) {
        Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
        intent.putExtra("chatId", chatId);
        holder.itemView.getContext().startActivity(intent);
    }
}