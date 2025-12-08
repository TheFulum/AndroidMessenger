package com.example.messenger.bottomnav.new_chat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messenger.R;
import com.example.messenger.databinding.FragmentNewChatBinding;
import com.example.messenger.users.User;
import com.example.messenger.users.UsersAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

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

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearch() {
        updateClearIcon(false);

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не используем
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearIcon(s.length() > 0);
                // Фильтруем при КАЖДОМ символе!
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не используем
            }
        });

        binding.searchEt.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isClearIconClicked(binding.searchEt, event)) {
                    binding.searchEt.setText("");
                    hideKeyboard();
                    return true;
                }
            }
            return false;
        });

        binding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void updateClearIcon(boolean show) {
        binding.searchEt.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_search,
                0,
                show ? R.drawable.ic_clear : 0,
                0
        );
    }

    private boolean isClearIconClicked(EditText editText, MotionEvent event) {
        if (editText.getCompoundDrawables()[2] == null) {
            return false;
        }

        float touchX = event.getX();
        int clearIconStart = editText.getWidth() - editText.getPaddingEnd() -
                editText.getCompoundDrawables()[2].getIntrinsicWidth();

        return touchX >= clearIconStart;
    }

    /**
     * Фильтрует и СРАЗУ обновляет список
     * Регистронезависимый поиск
     */
    private void filterUsers(String query) {
        String searchQuery = query.toLowerCase(Locale.ROOT).trim();

        filteredUsers.clear();

        if (searchQuery.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            for (User user : allUsers) {
                if (user.username.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                    filteredUsers.add(user);
                }
            }
        }

        // Обновление UI!
        adapter.notifyDataSetChanged();
    }

    private void hideKeyboard() {
        if (getContext() == null || binding.searchEt == null) return;

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchEt.getWindowToken(), 0);
        }
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

                            // ← ДОБАВЛЕНО: загружаем profileImageUrl
                            String profileImageUrl = snap.child("profileImageUrl").getValue(String.class);

                            allUsers.add(new User(uid, username, profileImageUrl));
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

                        if (getView() == null || binding == null) return; // 🔹 проверка

                        ArrayList<User> usersWithoutChats = new ArrayList<>();

                        for (User user : allUsers) {
                            String chatId1 = myUid + "_" + user.uid;
                            String chatId2 = user.uid + "_" + myUid;

                            if (!chatsSnapshot.hasChild(chatId1) && !chatsSnapshot.hasChild(chatId2)) {
                                usersWithoutChats.add(user);
                            }
                        }

                        allUsers.clear();
                        allUsers.addAll(usersWithoutChats);

                        // Применяем текущий фильтр поиска безопасно
                        String query = binding.searchEt != null ? binding.searchEt.getText().toString() : "";
                        filterUsers(query);
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}