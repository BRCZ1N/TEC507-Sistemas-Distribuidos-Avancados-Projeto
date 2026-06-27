package chat.sda.spring.dto;


import jakarta.validation.constraints.NotNull;

public class SendMessageDTO {

    @NotNull
    private String content;
    @NotNull
    private String senderId;

    public SendMessageDTO(String content, String senderId) {
        this.content = content;
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}