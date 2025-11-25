package com.example.messenger.chats;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import com.example.messenger.ChatActivity;
import com.example.messenger.R;

public class ChatsAdapter extends RecyclerView.Adapter<ChatViewHolder> {

    private ArrayList<Chat> chats;

    public ChatsAdapter(ArrayList<Chat> chats){
        this.chats = chats;
    }


    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_item_rv, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.chat_name_tv.setText(chats.get(position).getChat_name());

        String userId;
        if (!chats.get(position).getUserId1().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
            userId = chats.get(position).getUserId1();
        }else{
            userId = chats.get(position).getUserId2();
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("chatId", chats.get(position).getChat_id());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }
}
