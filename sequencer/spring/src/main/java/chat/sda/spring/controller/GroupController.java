package chat.sda.spring.controller;

import chat.sda.spring.model.Node;
import chat.sda.spring.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/join")
    public ResponseEntity<Void> join(@Valid @RequestBody Node node) {

        try {

            groupService.join(node);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Node>> getGroup() {
        return ResponseEntity.ok(groupService.getGroup());
    }
}