package chat.sda.spring.controller;

import chat.sda.spring.dto.AgreementMessageDTO;
import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.dto.SendMessageDTO;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.ProposalMessage;
import chat.sda.spring.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping(value = "chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody SendMessageDTO message) {

        try {
            chatService.sendMessage(new ChatMessage(message.getSenderId(), message.getContent()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @PostMapping("/proposal")
    public ResponseEntity<ProposalMessage> createProposal(@Valid @RequestBody ChatMessageDTO message) {

        try {
            ProposalMessage result = chatService.createProposal(new ChatMessage(message.getSenderId(), message.getId(), message.getContent(), message.getSequenceNumber(), message.getProcessSenderId(), message.getProcessProposerId(), message.getDeliverable()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @PostMapping("/agreement")
    public ResponseEntity<Void> sendAgreement(@Valid @RequestBody AgreementMessageDTO agreement) {

        try {
            chatService.receiveAgreement(agreement);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @GetMapping("/messages")
    public ResponseEntity<Queue<ChatMessage>> getMessage() {

        try {
            Queue<ChatMessage> message = chatService.getMessages();
            if (message != null) {
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