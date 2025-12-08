package com.example.messenger.message;

public class Message {

    private String id, ownerId, text, date;
    private long timestamp;

    // Поля для файлов
    private String fileUrl;
    private String fileType;
    private String fileName;
    private long fileSize;
    private long voiceDuration;
    private long videoDuration;

    // Пересылка
    private boolean isForwarded;
    private String forwardedFrom;

    // Редактирование
    private boolean isEdited;

    // НОВОЕ: Ответ на сообщение
    private String replyToMessageId;      // ID сообщения, на которое отвечаем
    private String replyToText;           // Текст исходного сообщения
    private String replyToOwnerName;      // Имя отправителя исходного сообщения
    private String replyToFileType;       // Тип файла (если в исходном был файл)

    public Message() {}

    public Message(String id, String ownerId, String text, String date, long timestamp) {
        this.id = id;
        this.ownerId = ownerId;
        this.text = text;
        this.date = date;
        this.timestamp = timestamp;
        this.isEdited = false;
    }

    public Message(String id, String ownerId, String text, String date, long timestamp,
                   String fileUrl, String fileType, String fileName, long fileSize,
                   long voiceDuration, long videoDuration) {
        this.id = id;
        this.ownerId = ownerId;
        this.text = text;
        this.date = date;
        this.timestamp = timestamp;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.voiceDuration = voiceDuration;
        this.videoDuration = videoDuration;
        this.isEdited = false;
    }

    // Getters и Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getVoiceDuration() { return voiceDuration; }
    public void setVoiceDuration(long voiceDuration) { this.voiceDuration = voiceDuration; }

    public long getVideoDuration() { return videoDuration; }
    public void setVideoDuration(long videoDuration) { this.videoDuration = videoDuration; }

    public boolean isForwarded() { return isForwarded; }
    public void setForwarded(boolean forwarded) { isForwarded = forwarded; }

    public String getForwardedFrom() { return forwardedFrom; }
    public void setForwardedFrom(String forwardedFrom) { this.forwardedFrom = forwardedFrom; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    // НОВОЕ: Getters и Setters для ответов
    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToText() { return replyToText; }
    public void setReplyToText(String replyToText) { this.replyToText = replyToText; }

    public String getReplyToOwnerName() { return replyToOwnerName; }
    public void setReplyToOwnerName(String replyToOwnerName) { this.replyToOwnerName = replyToOwnerName; }

    public String getReplyToFileType() { return replyToFileType; }
    public void setReplyToFileType(String replyToFileType) { this.replyToFileType = replyToFileType; }

    // НОВОЕ: Проверка, является ли сообщение ответом
    public boolean isReply() {
        return replyToMessageId != null && !replyToMessageId.isEmpty();
    }

    // Вспомогательные методы
    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    public boolean isImage() {
        return "image".equals(fileType);
    }

    public boolean isVideo() {
        return "video".equals(fileType);
    }

    public boolean isVoice() {
        return "voice".equals(fileType);
    }

    public boolean isDocument() {
        return "document".equals(fileType);
    }

    public String getFormattedVoiceDuration() {
        if (voiceDuration <= 0) return "0:00";
        int seconds = (int) (voiceDuration / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedVideoDuration() {
        if (videoDuration <= 0) return "0:00";
        int seconds = (int) (videoDuration / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}