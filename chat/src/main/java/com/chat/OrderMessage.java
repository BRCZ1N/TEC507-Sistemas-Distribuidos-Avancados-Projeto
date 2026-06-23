package com.chat;

import java.io.Serializable;

public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String senderId;
    private final String messageId;
    private final long sequenceNumber;

    public OrderMessage(String senderId, String messageId, long sequenceNumber) {
        this.senderId = senderId;
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}