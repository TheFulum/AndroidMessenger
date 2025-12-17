package com.example.messenger.data.models;

public class User {

    public String uid, username, profileImageUrl;

    public User() {
        // Пустой конструктор для Firebase
    }

    public User(String uid, String username) {
        this.uid = uid;
        this.username = username;
        this.profileImageUrl = null;
    }

    public User(String uid, String username, String profileImageUrl) {
        this.uid = uid;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }
}