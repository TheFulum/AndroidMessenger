package com.example.messenger.bottomnav.new_chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NewChatFragment extends Fragment {

    private FragmentNewChatBinding binding;

    private ArrayList<User> allUsers = new ArrayList<>();
    private ArrayList<User> filteredUsers = new ArrayList<>();
    private UsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentNewChatBinding.inflate(inflater, container, false);

        adapter = new UsersAdapter(filteredUsers);

        binding.usersRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.usersRv.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        binding.usersRv.setAdapter(adapter);

        loadUsers();
        setupSearch();

        return binding.getRoot();
    }

    private void loadUsers() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        allUsers.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String uid = snap.getKey();
                            if (uid == null || uid.equals(myUid)) continue;

                            String username = snap.child("username").getValue(String.class);
                            if (username == null) continue;

                            allUsers.add(new User(uid, username));
                        }

                        filterExistingChats(myUid);
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void filterExistingChats(String myUid) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Chats")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {

                        filteredUsers.clear();

                        for (User user : allUsers) {
                            String chatId1 = myUid + "_" + user.uid;
                            String chatId2 = user.uid + "_" + myUid;

                            if (!chatsSnapshot.hasChild(chatId1) && !chatsSnapshot.hasChild(chatId2)) {
                                filteredUsers.add(user);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void setupSearch() {

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = s.toString().toLowerCase().trim();

                filteredUsers.clear();

                for (User user : allUsers) {
                    if (user.username.toLowerCase().contains(query)) {
                        filteredUsers.add(user);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
