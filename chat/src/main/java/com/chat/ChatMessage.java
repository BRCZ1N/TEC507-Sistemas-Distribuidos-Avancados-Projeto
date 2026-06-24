package com.chat;

import java.io.Serializable;
import java.util.UUID;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String sender;
    private String id;
    private String content;

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.id = UUID.randomUUID().toString();
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSenderId(String senderId) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}