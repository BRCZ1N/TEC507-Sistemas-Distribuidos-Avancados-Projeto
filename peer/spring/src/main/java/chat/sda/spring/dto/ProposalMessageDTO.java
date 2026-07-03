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
    private Long processProposalId;

    public ProposalMessageDTO(String messageId, Long sequenceNumber, Long processSenderId, Long processProposalId) {
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
        this.processSenderId = processSenderId;
        this.processProposalId = processProposalId;
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

    public Long getProcessProposalId() {
        return processProposalId;
    }

    public void setProcessProposalId(Long processProposalId) {
        this.processProposalId = processProposalId;
    }
}