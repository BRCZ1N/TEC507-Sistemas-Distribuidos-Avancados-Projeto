package chat.sda.spring.dto;

import com.chat.ChatMessage;
import jakarta.validation.constraints.NotNull;

public class ChatMessageDTO {

    @NotNull
    private String content;

    public ChatMessageDTO(String content){
        this.content = content;

    }

    public String getContent() {
        return content;
    }

}