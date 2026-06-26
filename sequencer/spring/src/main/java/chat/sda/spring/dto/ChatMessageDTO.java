package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class ChatMessageDTO {

    @NotNull
    private String content;
    @NotNull
    private String id;

    public ChatMessageDTO(String content, String id) {
        this.content = content;
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public String getId() { return id; }
}