package com.example.messenger.message;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.ChatActivity;
import com.example.messenger.MediaViewerActivity;
import com.example.messenger.R;
import com.example.messenger.SelectChatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TEXT_MY = 0;
    private static final int TYPE_TEXT_OTHER = 1;
    private static final int TYPE_IMAGE_MY = 2;
    private static final int TYPE_IMAGE_OTHER = 3;
    private static final int TYPE_DOCUMENT_MY = 4;
    private static final int TYPE_DOCUMENT_OTHER = 5;
    private static final int TYPE_VOICE_MY = 6;
    private static final int TYPE_VOICE_OTHER = 7;
    private static final int TYPE_VIDEO_MY = 8;
    private static final int TYPE_VIDEO_OTHER = 9;
    private Context context;

    private List<Message> messages;
    private String chatId;
    private String currentUserId;

    private MediaPlayer currentPlayer;
    private VoiceMessageViewHolder currentPlayingHolder;

    public MessagesAdapter(List<Message> messages, String chatId) {
        this.messages = messages;
        this.chatId = chatId;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isMy = message.getOwnerId().equals(currentUserId);

        if (message.hasFile()) {
            if (message.isImage()) {
                return isMy ? TYPE_IMAGE_MY : TYPE_IMAGE_OTHER;
            } else if (message.isVideo()) {
                return isMy ? TYPE_VIDEO_MY : TYPE_VIDEO_OTHER;
            } else if (message.isVoice()) {
                return isMy ? TYPE_VOICE_MY : TYPE_VOICE_OTHER;
            } else {
                return isMy ? TYPE_DOCUMENT_MY : TYPE_DOCUMENT_OTHER;
            }
        } else {
            return isMy ? TYPE_TEXT_MY : TYPE_TEXT_OTHER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_TEXT_MY:
                return new TextMessageViewHolder(
                        inflater.inflate(R.layout.message_from_curr_user_rv_item, parent, false));
            case TYPE_TEXT_OTHER:
                return new TextMessageViewHolder(
                        inflater.inflate(R.layout.message_rv_item, parent, false));
            case TYPE_IMAGE_MY:
                return new ImageMessageViewHolder(
                        inflater.inflate(R.layout.message_with_image_my, parent, false));
            case TYPE_IMAGE_OTHER:
                return new ImageMessageViewHolder(
                        inflater.inflate(R.layout.message_with_image, parent, false));
            case TYPE_VIDEO_MY:
                return new VideoMessageViewHolder(
                        inflater.inflate(R.layout.message_with_video_my, parent, false));
            case TYPE_VIDEO_OTHER:
                return new VideoMessageViewHolder(
                        inflater.inflate(R.layout.message_with_video, parent, false));
            case TYPE_DOCUMENT_MY:
                return new DocumentMessageViewHolder(
                        inflater.inflate(R.layout.message_with_document_my, parent, false));
            case TYPE_DOCUMENT_OTHER:
                return new DocumentMessageViewHolder(
                        inflater.inflate(R.layout.message_with_document, parent, false));
            case TYPE_VOICE_MY:
                return new VoiceMessageViewHolder(
                        inflater.inflate(R.layout.message_with_voice, parent, false));
            case TYPE_VOICE_OTHER:
                return new VoiceMessageViewHolder(
                        inflater.inflate(R.layout.message_with_voice_other, parent, false));
            default:
                return new TextMessageViewHolder(
                        inflater.inflate(R.layout.message_rv_item, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isMyMessage = message.getOwnerId().equals(currentUserId);

        if (holder instanceof TextMessageViewHolder) {
            bindTextMessage((TextMessageViewHolder) holder, message, isMyMessage);
        } else if (holder instanceof ImageMessageViewHolder) {
            bindImageMessage((ImageMessageViewHolder) holder, message, isMyMessage);
        } else if (holder instanceof VideoMessageViewHolder) {
            bindVideoMessage((VideoMessageViewHolder) holder, message, isMyMessage);
        } else if (holder instanceof DocumentMessageViewHolder) {
            bindDocumentMessage((DocumentMessageViewHolder) holder, message, isMyMessage);
        } else if (holder instanceof VoiceMessageViewHolder) {
            bindVoiceMessage((VoiceMessageViewHolder) holder, message, isMyMessage);
        }
    }

    private void bindTextMessage(TextMessageViewHolder holder, Message message, boolean isMyMessage) {
        // Пересылка
        if (message.isForwarded() && message.getForwardedFrom() != null) {
            holder.forwardedTv.setVisibility(View.VISIBLE);
            holder.forwardedTv.setText("📩 Forwarded from " + message.getForwardedFrom());
        } else {
            holder.forwardedTv.setVisibility(View.GONE);
        }

        // НОВОЕ: Ответ
        if (message.isReply()) {
            holder.replyBlock.setVisibility(View.VISIBLE);
            holder.replyOwnerNameTv.setText(message.getReplyToOwnerName());

            // Определяем текст для отображения
            String replyDisplayText;
            if (message.getReplyToFileType() != null && !message.getReplyToFileType().isEmpty()) {
                switch (message.getReplyToFileType()) {
                    case "image":
                        replyDisplayText = "📷 Фото";
                        break;
                    case "video":
                        replyDisplayText = "🎥 Видео";
                        break;
                    case "voice":
                        replyDisplayText = "🎤 Голосовое сообщение";
                        break;
                    default:
                        replyDisplayText = "📄 " + message.getReplyToText();
                        break;
                }
            } else {
                replyDisplayText = message.getReplyToText();
            }

            holder.replyTextTv.setText(replyDisplayText);

            holder.replyBlock.setOnClickListener(v -> onReplyClick(v.getContext(), message));
        } else {
            holder.replyBlock.setVisibility(View.GONE);
        }

        holder.messageTv.setText(cleanForwardedText(message.getText()));
        holder.dateTv.setText(message.getDate());

        // Метка "изменено"
        holder.editedTv.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void bindImageMessage(ImageMessageViewHolder holder, Message message, boolean isMyMessage) {
        if (message.isForwarded() && message.getForwardedFrom() != null) {
            holder.forwardedTv.setVisibility(View.VISIBLE);
            holder.forwardedTv.setText("📩 Forwarded from " + message.getForwardedFrom());
        } else {
            holder.forwardedTv.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
                .load(message.getFileUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.imageView);

        String displayText = cleanForwardedText(message.getText());
        if (displayText != null && !displayText.isEmpty()) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(displayText);
        } else {
            holder.messageTv.setVisibility(View.GONE);
        }

        holder.dateTv.setText(message.getDate());

        holder.imageView.setOnClickListener(v -> {
            openMediaFullscreen(v.getContext(), message.getFileUrl(), "image");
        });

        holder.downloadBtn.setOnClickListener(v -> {
            downloadFile(v.getContext(), message.getFileUrl(), message.getFileName());
        });

        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void bindVideoMessage(VideoMessageViewHolder holder, Message message, boolean isMyMessage) {
        if (message.isForwarded() && message.getForwardedFrom() != null) {
            holder.forwardedTv.setVisibility(View.VISIBLE);
            holder.forwardedTv.setText("📩 Forwarded from " + message.getForwardedFrom());
        } else {
            holder.forwardedTv.setVisibility(View.GONE);
        }

        // Загружаем превью видео (первый кадр)
        Glide.with(holder.itemView.getContext())
                .load(message.getFileUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.videoThumbnail);

        // Отображаем длительность
        holder.videoDurationTv.setText(message.getFormattedVideoDuration());

        String displayText = cleanForwardedText(message.getText());
        if (displayText != null && !displayText.isEmpty()) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(displayText);
        } else {
            holder.messageTv.setVisibility(View.GONE);
        }

        holder.dateTv.setText(message.getDate());

        // Клик по превью открывает видео на весь экран
        holder.videoThumbnail.setOnClickListener(v -> {
            openMediaFullscreen(v.getContext(), message.getFileUrl(), "video");
        });

        holder.downloadBtn.setOnClickListener(v -> {
            downloadFile(v.getContext(), message.getFileUrl(), message.getFileName());
        });

        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void bindDocumentMessage(DocumentMessageViewHolder holder, Message message, boolean isMyMessage) {
        if (message.isForwarded() && message.getForwardedFrom() != null) {
            holder.forwardedTv.setVisibility(View.VISIBLE);
            holder.forwardedTv.setText("📩 Forwarded from " + message.getForwardedFrom());
        } else {
            holder.forwardedTv.setVisibility(View.GONE);
        }

        holder.fileNameTv.setText(message.getFileName());
        holder.fileSizeTv.setText(formatFileSize(message.getFileSize()));

        String displayText = cleanForwardedText(message.getText());
        if (displayText != null && !displayText.isEmpty()) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(displayText);
        } else {
            holder.messageTv.setVisibility(View.GONE);
        }

        holder.dateTv.setText(message.getDate());

        holder.downloadBtn.setOnClickListener(v -> {
            downloadFile(v.getContext(), message.getFileUrl(), message.getFileName());
        });

        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void bindVoiceMessage(VoiceMessageViewHolder holder, Message message, boolean isMyMessage) {
        if (message.isForwarded() && message.getForwardedFrom() != null) {
            holder.forwardedTv.setVisibility(View.VISIBLE);
            holder.forwardedTv.setText("📩 Forwarded from " + message.getForwardedFrom());
        } else {
            holder.forwardedTv.setVisibility(View.GONE);
        }

        holder.voiceDurationTv.setText(message.getFormattedVoiceDuration());
        holder.dateTv.setText(message.getDate());
        holder.seekBar.setProgress(0);
        holder.playPauseBtn.setImageResource(R.drawable.ic_play);

        holder.playPauseBtn.setOnClickListener(v -> {
            toggleVoicePlayback(holder, message);
        });

        holder.itemView.setOnLongClickListener(v -> {
            showMessageActionsSheet(v, message, isMyMessage);
            return true;
        });
    }

    private void toggleVoicePlayback(VoiceMessageViewHolder holder, Message message) {
        if (currentPlayer != null && currentPlayingHolder != holder) {
            stopCurrentPlayback();
        }

        if (currentPlayer != null && currentPlayingHolder == holder) {
            if (currentPlayer.isPlaying()) {
                currentPlayer.pause();
                holder.playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                currentPlayer.start();
                holder.playPauseBtn.setImageResource(R.drawable.ic_pause);
                updateSeekBar(holder, message);
            }
        } else {
            playVoiceMessage(holder, message);
        }
    }

    private void playVoiceMessage(VoiceMessageViewHolder holder, Message message) {
        try {
            currentPlayer = new MediaPlayer();
            currentPlayer.setDataSource(message.getFileUrl());
            currentPlayer.prepareAsync();

            currentPlayer.setOnPreparedListener(mp -> {
                mp.start();
                currentPlayingHolder = holder;
                holder.playPauseBtn.setImageResource(R.drawable.ic_pause);
                updateSeekBar(holder, message);
            });

            currentPlayer.setOnCompletionListener(mp -> {
                holder.playPauseBtn.setImageResource(R.drawable.ic_play);
                holder.seekBar.setProgress(0);
                holder.voiceDurationTv.setText(message.getFormattedVoiceDuration());
                releasePlayer();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(holder.itemView.getContext(),
                    "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSeekBar(VoiceMessageViewHolder holder, Message message) {
        if (currentPlayer == null || !currentPlayer.isPlaying()) return;

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentPlayer != null && currentPlayer.isPlaying()) {
                    int currentPos = currentPlayer.getCurrentPosition();
                    int duration = currentPlayer.getDuration();

                    int progress = (int) ((currentPos * 100.0) / duration);
                    holder.seekBar.setProgress(progress);

                    int remainingSeconds = (duration - currentPos) / 1000;
                    int minutes = remainingSeconds / 60;
                    int seconds = remainingSeconds % 60;
                    holder.voiceDurationTv.setText(String.format("%d:%02d", minutes, seconds));

                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(runnable);
    }

    private void stopCurrentPlayback() {
        if (currentPlayer != null) {
            if (currentPlayer.isPlaying()) {
                currentPlayer.stop();
            }
            if (currentPlayingHolder != null) {
                currentPlayingHolder.playPauseBtn.setImageResource(R.drawable.ic_play);
                currentPlayingHolder.seekBar.setProgress(0);
            }
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (currentPlayer != null) {
            currentPlayer.release();
            currentPlayer = null;
        }
        currentPlayingHolder = null;
    }

    private String cleanForwardedText(String text) {
        if (text == null) return null;
        if (text.startsWith("📩 Forwarded from")) {
            int colonIndex = text.indexOf(":\n");
            if (colonIndex != -1 && colonIndex + 2 < text.length()) {
                return text.substring(colonIndex + 2);
            }
        }
        return text;
    }

    private void openMediaFullscreen(Context context, String mediaUrl, String mediaType) {
        Intent intent = new Intent(context, MediaViewerActivity.class);
        intent.putExtra("mediaUrl", mediaUrl);
        intent.putExtra("mediaType", mediaType);
        intent.putExtra("title", mediaType.equals("image") ? "Изображение" : "Видео");
        context.startActivity(intent);
    }

    private void downloadFile(Context context, String fileUrl, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(fileName);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            Toast.makeText(context, "Скачивание начато", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка скачивания", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showMessageActionsSheet(View view, Message message, boolean isMyMessage) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(view.getContext());
        View sheetView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.bottom_sheet_message_actions, null);

        LinearLayout actionReply = sheetView.findViewById(R.id.action_reply);      // НОВОЕ
        LinearLayout actionForward = sheetView.findViewById(R.id.action_forward);
        LinearLayout actionDelete = sheetView.findViewById(R.id.action_delete);
        LinearLayout actionEdit = sheetView.findViewById(R.id.action_edit);

        // НОВОЕ: Обработчик для ответа (всегда показываем)
        actionReply.setOnClickListener(v -> {
            bottomSheet.dismiss();
            handleReply(view, message);
        });

        actionForward.setOnClickListener(v -> {
            bottomSheet.dismiss();
            forwardMessage(view, message);
        });

        if (isMyMessage) {
            actionDelete.setVisibility(View.VISIBLE);
            actionDelete.setOnClickListener(v -> {
                bottomSheet.dismiss();
                showDeleteConfirmation(view, message);
            });
        } else {
            actionDelete.setVisibility(View.GONE);
        }

        // Показываем "Изменить" только для своих текстовых сообщений без файлов
        if (isMyMessage && !message.hasFile() && message.getText() != null && !message.getText().isEmpty()) {
            actionEdit.setVisibility(View.VISIBLE);
            actionEdit.setOnClickListener(v -> {
                bottomSheet.dismiss();
                showEditDialog(view.getContext(), message);
            });
        } else {
            actionEdit.setVisibility(View.GONE);
        }

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void showEditDialog(Context context, Message message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Изменить сообщение");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(message.getText());
        input.setSelection(input.getText().length());

        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (newText.isEmpty()) {
                Toast.makeText(context, "Сообщение не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newText.equals(message.getText())) {
                return;  // Нет изменений
            }
            updateMessageText(context, message, newText);
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Новое: Обновление текста в Firebase
    private void updateMessageText(Context context, Message message, String newText) {
        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .child("messages")
                .child(message.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("text", newText);
        updates.put("isEdited", true);

        msgRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Сообщение изменено", Toast.LENGTH_SHORT).show();
                    updateLastMessagePreviewIfNeeded(message, newText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка изменения", Toast.LENGTH_SHORT).show();
                });
    }

    // Новое: Обновление предпросмотра последнего сообщения, если это было последнее
    private void updateLastMessagePreviewIfNeeded(Message message, String newText) {
        if (messages.isEmpty() || !messages.get(messages.size() - 1).getId().equals(message.getId())) {
            return;
        }

        String preview = newText.length() > 50 ? newText.substring(0, 47) + "..." : newText;

        Map<String, Object> update = new HashMap<>();
        update.put("lastMessagePreview", preview);

        FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId)
                .updateChildren(update);
    }

    private void forwardMessage(View view, Message message) {
        Intent intent = new Intent(view.getContext(), SelectChatActivity.class);

        intent.putExtra("messageText", message.getText());
        intent.putExtra("sourceChatId", chatId);

        if (message.hasFile()) {
            intent.putExtra("fileUrl", message.getFileUrl());
            intent.putExtra("fileType", message.getFileType());
            intent.putExtra("fileName", message.getFileName());
            intent.putExtra("fileSize", message.getFileSize());

            if (message.isVoice()) {
                intent.putExtra("voiceDuration", message.getVoiceDuration());
            } else if (message.isVideo()) {
                intent.putExtra("videoDuration", message.getVideoDuration());
            }
        }

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
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder == currentPlayingHolder) {
            stopCurrentPlayback();
        }
    }

    static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTv, dateTv, forwardedTv, editedTv;
        // НОВОЕ: Элементы блока ответа
        LinearLayout replyBlock;
        TextView replyOwnerNameTv, replyTextTv;

        TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            forwardedTv = itemView.findViewById(R.id.forwarded_tv);
            editedTv = itemView.findViewById(R.id.edited_tv);

            // НОВОЕ
            replyBlock = itemView.findViewById(R.id.reply_block);
            replyOwnerNameTv = itemView.findViewById(R.id.reply_owner_name_tv);
            replyTextTv = itemView.findViewById(R.id.reply_text_tv);
        }
    }

    static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView messageTv, dateTv, forwardedTv;
        Button downloadBtn;

        ImageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.message_image);
            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            downloadBtn = itemView.findViewById(R.id.download_btn);
            forwardedTv = itemView.findViewById(R.id.forwarded_tv);
        }
    }

    static class VideoMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView videoThumbnail;
        TextView videoDurationTv, messageTv, dateTv, forwardedTv;
        Button downloadBtn;

        VideoMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoDurationTv = itemView.findViewById(R.id.video_duration_tv);
            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            downloadBtn = itemView.findViewById(R.id.download_btn);
            forwardedTv = itemView.findViewById(R.id.forwarded_tv);
        }
    }

    static class DocumentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTv, fileSizeTv, messageTv, dateTv, forwardedTv;
        Button downloadBtn;

        DocumentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTv = itemView.findViewById(R.id.file_name_tv);
            fileSizeTv = itemView.findViewById(R.id.file_size_tv);
            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            downloadBtn = itemView.findViewById(R.id.download_btn);
            forwardedTv = itemView.findViewById(R.id.forwarded_tv);
        }
    }

    static class VoiceMessageViewHolder extends RecyclerView.ViewHolder {
        ImageButton playPauseBtn;
        SeekBar seekBar;
        TextView voiceDurationTv, dateTv, forwardedTv;

        VoiceMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            playPauseBtn = itemView.findViewById(R.id.play_pause_btn);
            seekBar = itemView.findViewById(R.id.voice_seekbar);
            voiceDurationTv = itemView.findViewById(R.id.voice_duration_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            forwardedTv = itemView.findViewById(R.id.forwarded_tv);
        }
    }
    private void handleReply(View view, Message message) {
        // Получаем имя отправителя
        String ownerId = message.getOwnerId();

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(ownerId)
                .child("username")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String ownerName = snapshot.getValue(String.class);
                    if (ownerName == null) {
                        ownerName = "Пользователь";
                    }

                    // Вызываем метод в ChatActivity
                    if (view.getContext() instanceof ChatActivity) {
                        ((ChatActivity) view.getContext()).showReplyBlock(message, ownerName);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                });
    }

    private void onReplyClick(Context ctx, Message message) {
        if (!message.isReply()) return;

        Toast.makeText(ctx, "Переход к сообщению", Toast.LENGTH_SHORT).show();

        // Ищем исходное сообщение
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(message.getReplyToMessageId())) {

                // ChatActivity умеет скроллить
                if (ctx instanceof ChatActivity) {
                    ((ChatActivity) ctx).scrollToMessage(i);
                }
                break;
            }
        }
    }

}