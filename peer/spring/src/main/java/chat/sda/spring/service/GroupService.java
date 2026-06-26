package chat.sda.spring.service;

import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class GroupService {

    private final ArrayList<Node> group;
    private final Node nodeSequencer;
    private final NodeConfig nodeConfig;

    public GroupService(NodeConfig nodeConfig) {

        this.group = new ArrayList<>();
        this.nodeSequencer = new Node("P1", "localhost", 6000);
        this.nodeConfig = nodeConfig;
    }

    public ArrayList<Node> getGroup() {

        return group;
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

    public synchronized void refreshGroup(ArrayList<Node> newGroup) {

        this.group.clear();
        this.group.addAll(newGroup);

    }


}
