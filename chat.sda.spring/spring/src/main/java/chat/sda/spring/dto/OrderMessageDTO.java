public class OrderMessageDTO {

    @NotNull
    private final String messageId;
    @NotNull
    private final long sequenceNumber;

    public String getMessageId() {
        return messageId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}