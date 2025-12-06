package com.example.messenger.message;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.R;
import com.example.messenger.SelectChatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Objects;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String chatId;
    private String currentUserId;

    public MessagesAdapter(List<Message> messages, String chatId) {
        this.messages = messages;
        this.chatId = chatId;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        holder.messageTv.setText(message.getText());
        holder.dateTv.setText(message.getDate());

        // Определяем, свое ли сообщение
        boolean isMyMessage = message.getOwnerId().equals(currentUserId);

        // Долгое нажатие для показа меню
        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void showMessageActionsSheet(View view, Message message, boolean isMyMessage) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(view.getContext());
        View sheetView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.bottom_sheet_message_actions, null);

        LinearLayout actionForward = sheetView.findViewById(R.id.action_forward);
        LinearLayout actionDelete = sheetView.findViewById(R.id.action_delete);

        // Кнопка "Переслать" - доступна всегда
        actionForward.setOnClickListener(v -> {
            bottomSheet.dismiss();
            forwardMessage(view, message);
        });

        // Кнопка "Удалить" - только для своих сообщений
        if (isMyMessage) {
            actionDelete.setVisibility(View.VISIBLE);
            actionDelete.setOnClickListener(v -> {
                bottomSheet.dismiss();
                showDeleteConfirmation(view, message);
            });
        } else {
            actionDelete.setVisibility(View.GONE);
        }

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void forwardMessage(View view, Message message) {
        Intent intent = new Intent(view.getContext(), SelectChatActivity.class);
        intent.putExtra("messageText", message.getText());
        intent.putExtra("sourceChatId", chatId);
        view.getContext().startActivity(intent);
    }

    private void showDeleteConfirmation(View view, Message message) {
        new androidx.appcompat.app.AlertDialog.Builder(view.getContext())
                .setTitle("Удалить сообщение?")
                .setMessage("Это действие нельзя отменить")
                .setPositiveButton("Удалить", (dialog, which) -> deleteMessage(view, message))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteMessage(View view, Message message) {
        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .child(message.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(view.getContext(), "Сообщение удалено", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getOwnerId().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()))
            return R.layout.message_from_curr_user_rv_item;
        else
            return R.layout.message_rv_item;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageTv, dateTv;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
        }
    }
}