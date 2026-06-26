package chat.sda.spring.controller;

import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.dto.OrderMessageDTO;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.OrderMessage;
import chat.sda.spring.service.ChatService;
import chat.sda.spring.utils.NodeConfig;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/chat")
public class ChatController {

    private final ChatService chatService;
    private final NodeConfig nodeConfig;

    public ChatController(ChatService chatService, NodeConfig nodeConfig){
        this.chatService = chatService;
        this.nodeConfig = nodeConfig;
    }

    @PostMapping
    public ResponseEntity<Void> addChatMessage(@Valid @RequestBody ChatMessageDTO message) {

        try {
            chatService.chatDeliver(new ChatMessage(nodeConfig.getSelf().getId(),message.getContent()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @PostMapping("/order")
    public ResponseEntity<Void> addOrderMessage(@Valid @RequestBody OrderMessageDTO order) {

        try {
            chatService.orderDeliver(new OrderMessage(order.getMessageId(), order.getSequenceNumber()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @GetMapping
    public ResponseEntity<ChatMessageDTO> getMessage() {

        try {
            return ResponseEntity.ok(chatService.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

}