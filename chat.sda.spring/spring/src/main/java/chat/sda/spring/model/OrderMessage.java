public class OrderMessage {

    private final String messageId;
    private final long sequenceNumber;

    public OrderMessage(String messageId, long sequenceNumber) {
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
    }

    public String getMessageId() {
        return messageId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}