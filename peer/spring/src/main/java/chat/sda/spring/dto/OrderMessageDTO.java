package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class OrderMessageDTO {

    @NotNull
    private String messageId;
    @NotNull
    private long sequenceNumber;

    public String getMessageId() {
        return messageId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}