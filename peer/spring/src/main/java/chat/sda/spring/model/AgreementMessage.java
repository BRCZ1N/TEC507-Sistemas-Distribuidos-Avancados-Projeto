package chat.sda.spring.model;

public class AgreementMessage {

    private String messageId;
    private Long sequenceNumber;
    private Long processProposerId;

    public AgreementMessage(String messageId, Long sequenceNumber, Long processProposerId) {
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
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

    public Long getProcessProposerId() {
        return processProposerId;
    }

    public void setProcessProposerId(Long processProposerId) {
        this.processProposerId = processProposerId;
    }
}