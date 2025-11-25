package com.example.messenger.bottomnav.new_chat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.databinding.FragmentNewChatBinding;
import com.example.messenger.users.User;
import com.example.messenger.users.UsersAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NewChatFragment extends Fragment {

    private FragmentNewChatBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewChatBinding.inflate(inflater, container, false);



        ArrayList<User> users = new ArrayList<>();
        UsersAdapter adapter = new UsersAdapter(users);

        binding.usersRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.usersRv.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        binding.usersRv.setAdapter(adapter);

        FirebaseDatabase.getInstance().getReference().child("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        users.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            if (userSnapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                continue;
                            }

                            String uid = userSnapshot.getKey();
                            String username = userSnapshot.child("username").getValue(String.class);

                            if (username != null) {
                                users.add(new User(uid, username));
                            }
                        }

                        adapter.notifyDataSetChanged(); // Обновляем RecyclerView
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });


        return binding.getRoot();
    }

}
