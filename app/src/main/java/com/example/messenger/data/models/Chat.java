package com.example.messenger.data.models;

import java.util.HashMap;
import java.util.Map;

public class Chat {

    private String user1;
    private String user2;
    private String lastActivity;
    private Map<String, Boolean> blockedUsers;

    public Chat() {
        // нужен для Firebase
        this.blockedUsers = new HashMap<>();
    }

    public Chat(String user1, String user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.lastActivity = "";
        this.blockedUsers = new HashMap<>();
    }

    public Chat(String user1, String user2, String lastActivity) {
        this.user1 = user1;
        this.user2 = user2;
        this.lastActivity = lastActivity;
        this.blockedUsers = new HashMap<>();
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

    public Map<String, Boolean> getBlockedUsers() {
        return blockedUsers;
    }

    public void setBlockedUsers(Map<String, Boolean> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    // Проверка, заблокирован ли пользователь
    public boolean isUserBlocked(String userId) {
        if (blockedUsers == null) return false;
        Boolean blocked = blockedUsers.get(userId);
        return blocked != null && blocked;
    }
}