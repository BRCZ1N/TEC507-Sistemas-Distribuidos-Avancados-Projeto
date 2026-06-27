package chat.sda.spring.dto;


import jakarta.validation.constraints.NotNull;

public class SendMessageDTO {

    @NotNull
    private String content;

    public SendMessageDTO(String content){
        this.content = content;

    }

    public String getContent() {
        return content;
    }

}