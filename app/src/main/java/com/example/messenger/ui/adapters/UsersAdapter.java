package com.example.messenger.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.R;
import com.example.messenger.ui.activities.UserProfileActivity;
import com.example.messenger.data.models.User;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
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

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username_tv;
        CircleImageView profileImage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            username_tv = itemView.findViewById(R.id.username_tv);
            profileImage = itemView.findViewById(R.id.profile_iv);
        }
    }
}