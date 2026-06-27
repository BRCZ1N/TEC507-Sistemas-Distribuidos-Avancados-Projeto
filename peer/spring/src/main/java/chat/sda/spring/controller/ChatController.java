package chat.sda.spring.controller;

import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.dto.OrderMessageDTO;
import chat.sda.spring.dto.ReceiveMessageDTO;
import chat.sda.spring.dto.SendMessageDTO;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.OrderMessage;
import chat.sda.spring.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    @PostMapping("/multicast")
    public ResponseEntity<Void> multicast(@Valid @RequestBody SendMessageDTO message) {

        try {
            chatService.multiCast(new ChatMessage(message.getSenderId(),message.getContent()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @PostMapping("/deliver")
    public ResponseEntity<Void> deliver(@Valid @RequestBody ChatMessageDTO message) {

        try {
            chatService.bDeliver(new ChatMessage(message.getSenderId(),message.getId(),message.getContent()));
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
    public ResponseEntity<ReceiveMessageDTO> getMessage() {

        try {
            ReceiveMessageDTO message = chatService.getMessage();
            if(message != null){

                return ResponseEntity.ok(message);

            }
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

}