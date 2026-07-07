package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class ChatMessageDTO {

    @NotNull
    private String senderId;
    @NotNull
    private String id;
    @NotNull
    private String content;
    private Long sequenceNumber;
    @NotNull
    private Long processSenderId;
    private Long processProposerId;
    @NotNull
    private Boolean isDeliverable;


    public Boolean getDeliverable() {
        return isDeliverable;
    }

    public void setDeliverable(Boolean deliverable) {
        isDeliverable = deliverable;
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

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Long getProcessSenderId() {
        return processSenderId;
    }

    public void setProcessSenderId(Long processSenderId) {
        this.processSenderId = processSenderId;
    }

    public Long getProcessProposerId() {
        return processProposerId;
    }

    public void setProcessProposerId(Long processProposerId) {
        this.processProposerId = processProposerId;
    }
}