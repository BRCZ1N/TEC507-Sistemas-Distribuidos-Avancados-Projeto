package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class AgreementMessageDTO {

    @NotNull
    private String messageId;
    @NotNull
    private Long sequenceNumber;


    public AgreementMessageDTO(String messageId, Long sequenceNumber) {
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