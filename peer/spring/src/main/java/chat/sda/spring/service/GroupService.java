package chat.sda.spring.service;

import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupService {

    private final Map<String,Node> group;
    private final Node nodeSequencer;
    private final NodeConfig nodeConfig;

    public GroupService(NodeConfig nodeConfig) {

        this.group = new ConcurrentHashMap<>();
        this.nodeSequencer = new Node("P1", "localhost", 6000);
        this.nodeConfig = nodeConfig;
    }

    public ArrayList<Node> getGroup() {
        return new ArrayList<>(group.values());
    }

    public void join() {

        new Thread(() -> {
            RestTemplate rest = new RestTemplate();

            rest.postForEntity(
                    "http://" + nodeSequencer.getHost() + ":" + nodeSequencer.getPort() + "/group/join",
                    nodeConfig.getSelf(),
                    Void.class
            );
        }).start();
    }

    public void refreshGroup(ArrayList<Node> newGroup) {

        for (Node node : newGroup) {

            if(!group.containsKey(node.getId())){

                group.put(node.getId(), node);

            }
        }
    }

}
