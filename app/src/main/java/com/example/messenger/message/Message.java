package com.example.messenger.message;

public class Message {

    private String id, ownerId, text, date;
    private long timestamp;  // ← Добавь это (long для millis)

    public Message() {} // обязательно для Firebase

    public Message(String id, String ownerId, String text, String date, long timestamp) {
        this.id = id;
        this.ownerId = ownerId;
        this.text = text;
        this.date = date;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }  // ← Getter
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }  // ← Setter
}