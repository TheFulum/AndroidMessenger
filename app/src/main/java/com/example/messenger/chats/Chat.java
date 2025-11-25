package com.example.messenger.chats;

public class Chat {

    private String chat_id;
    private String user1;
    private String user2;

    public Chat() {
        // нужен Firebase
    }

    public Chat(String chat_id, String user1, String user2) {
        this.chat_id = chat_id;
        this.user1 = user1;
        this.user2 = user2;
    }

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
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
}
