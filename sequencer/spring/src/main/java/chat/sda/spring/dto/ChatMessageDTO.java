package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class ChatMessageDTO {

    @NotNull
    private String id;
    @NotNull
    private String senderId;
    @NotNull
    private String content;

    public ChatMessageDTO(String id, String senderId, String content) {
        this.id = id;
        this.senderId = senderId;
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