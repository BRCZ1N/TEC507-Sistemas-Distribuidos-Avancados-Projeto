package com.chat;

import java.io.Serializable;
import java.util.UUID;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String senderId;
    private String id;
    private String content;

    public ChatMessage(String senderId, String content) {
        this.senderId = senderId;
        this.id = UUID.randomUUID().toString();
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}