package chat.sda.spring.controller;

import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.service.SequencerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/chat")
public class SequencerController {

    private final SequencerService sequenceService;

    public SequencerController(SequencerService sequencerService){
        this.sequenceService = sequencerService;
    }

    @PostMapping("/deliver")
    public ResponseEntity<Void> requestOrder(@Valid @RequestBody ChatMessageDTO message) {

        try {
            sequenceService.bDeliver(message);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

}