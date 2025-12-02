package com.example.messenger.chats;

public class Chat {

    private String user1;
    private String user2;
    private String lastActivity; // Добавьте это поле

    public Chat() {
        // нужен для Firebase
    }

    public Chat(String user1, String user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.lastActivity = ""; // Инициализируем пустой строкой
    }

    public Chat(String user1, String user2, String lastActivity) {
        this.user1 = user1;
        this.user2 = user2;
        this.lastActivity = lastActivity;
    }

    public String getUser1() {
        return user1;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public String getUser2() {
        return user2;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }
}