package chat.sda.spring.controller;

import chat.sda.spring.model.Node;
import chat.sda.spring.service.GroupService;
import chat.sda.spring.utils.NodeConfig;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/group")
public class GroupController {

    private final GroupService groupService;
    private final NodeConfig nodeConfig;

    public GroupController(GroupService groupService, NodeConfig nodeConfig) {

        this.groupService = groupService;
        this.nodeConfig = nodeConfig;
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@RequestBody ArrayList<Node> newGroup) {
        try {
            groupService.refreshGroup(newGroup);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
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

    @PostMapping("/election")
    public ResponseEntity<String> receiveElection(@RequestParam Long senderNode) {

        if (nodeConfig.getSelf().getId() > senderNode) {
            groupService.initBullyVote();
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @PostMapping("/coordinator")
    public ResponseEntity<Void> receiveCoordinator(@RequestBody Node newNodeLeader) {
        groupService.refreshCoordinator(newNodeLeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/leader")
    public ResponseEntity<Node> getGroupLeader() {
        Node leader = groupService.getLeader();
        if (leader == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(leader);
    }

    @GetMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat() {
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ArrayList<Node>> getGroup() {
        return ResponseEntity.ok(groupService.getGroup());
    }
}