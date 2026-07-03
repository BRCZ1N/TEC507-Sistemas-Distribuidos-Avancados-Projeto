package chat.sda.spring.model;

public class AgreementMessage {

    private String messageId;
    private Long sequenceNumber;

    public AgreementMessage(String messageId, Long sequenceNumber) {
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
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

}