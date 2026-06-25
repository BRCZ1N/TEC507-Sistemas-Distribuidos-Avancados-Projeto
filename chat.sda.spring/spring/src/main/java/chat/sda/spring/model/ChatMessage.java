package com.chat;

import java.io.Serializable;
import java.util.UUID;

public class ChatMessage {

    private String senderId;
    private String id;
    private String content;

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.id = UUID.randomUUID().toString();
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}