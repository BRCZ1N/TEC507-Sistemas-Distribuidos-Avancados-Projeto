package chat.sda.spring.model;

import java.util.UUID;

public class ChatMessage {

    private String senderId;
    private String id;
    private String content;

    public ChatMessage(String senderId, String content) {
        this.senderId = senderId;
        this.id = UUID.randomUUID().toString();
        this.content = content;
    }

    public ChatMessage(String senderId, String id, String content) {
        this.senderId = senderId;
        this.id = id;
        this.content = content;
    }

    public String getSenderId() { return senderId; }

    public String getId() { return id; }

    public String getContent() { return content; }

}