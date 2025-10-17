package com.example.messenger.users;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.R;


public class UserViewHolder extends RecyclerView.ViewHolder {

    TextView username_tv;

    public UserViewHolder(@NonNull View itemView) {
        super(itemView);

        username_tv = itemView.findViewById(R.id.username_tv);
    }
}
