package com.chat;

import java.io.Serializable;
import java.util.UUID;

public class ChatMessageDTO {

    @NotNull
    private String senderId;
    @NotNull
    private String id;
    private String content;

    public String getSenderId() {
        return senderId;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }


}