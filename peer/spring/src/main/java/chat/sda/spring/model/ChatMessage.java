package chat.sda.spring.model;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public class ChatMessage {

    private String senderId;
    private String id;
    private String content;
    private Long sequenceNumber;
    private Long processSenderId;
    private Long processProposalId;
    private Boolean isDeliverable;

    public ChatMessage(String senderId, String id, String content, Long sequenceNumber, Long processSenderId, Long processProposalId, Boolean isDeliverable) {
        this.senderId = senderId;
        this.id = id;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
        this.processSenderId = processSenderId;
        this.processProposalId = processProposalId;
        this.isDeliverable = isDeliverable;
    }

    public ChatMessage(String senderId, String content) {
        this.senderId = senderId;
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.isDeliverable = false;
    }

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

    public Long getProcessProposalId() {
        return processProposalId;
    }

    public void setProcessProposalId(Long processProposalId) {
        this.processProposalId = processProposalId;
    }
}
