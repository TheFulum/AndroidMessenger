package com.example.messenger.users;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.R;
import com.example.messenger.UserProfileActivity;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UserViewHolder> {

    private ArrayList<User> users;

    public UsersAdapter(ArrayList<User> users) {
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
            Intent intent = new Intent(v.getContext(), UserProfileActivity.class);
            intent.putExtra("userId", user.uid);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}