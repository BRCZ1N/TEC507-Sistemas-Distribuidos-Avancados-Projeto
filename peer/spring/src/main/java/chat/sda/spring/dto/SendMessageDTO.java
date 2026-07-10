package chat.sda.spring.dto;


import jakarta.validation.constraints.NotNull;

public class SendMessageDTO {

    @NotNull
    private String content;
    @NotNull
    private String senderId;
    private Long artificialDelay;

    public SendMessageDTO(String content, String senderId, Long artificialDelay) {
        this.content = content;
        this.senderId = senderId;
        this.artificialDelay = artificialDelay;
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

    public Long getArtificialDelay() {
        return artificialDelay;
    }

    public void setArtificialDelay(Long artificialDelay) {
        this.artificialDelay = artificialDelay;
    }
}