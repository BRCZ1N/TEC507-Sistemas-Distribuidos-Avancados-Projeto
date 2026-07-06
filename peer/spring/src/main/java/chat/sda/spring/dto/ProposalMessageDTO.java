package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class ProposalMessageDTO {

    @NotNull
    private String messageId;
    @NotNull
    private Long sequenceNumber;
    @NotNull
    private Long processSenderId;
    @NotNull
    private Long processProposerId;

    public ProposalMessageDTO(String messageId, Long sequenceNumber, Long processSenderId, Long processProposerId) {
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
        this.processSenderId = processSenderId;
        this.processProposerId = processProposerId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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