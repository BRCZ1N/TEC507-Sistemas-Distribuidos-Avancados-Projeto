package chat.sda.spring.service;

import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupService {

    private final ArrayList<Node> group;
    private final NodeConfig nodeConfig;

    public GroupService(NodeConfig nodeConfig) {

        this.group =  new ArrayList<>();
        this.nodeConfig = nodeConfig;
        this.group.add(nodeConfig.getSelf());

    }

    public synchronized void join(Node node) {

        boolean exists = group.stream().anyMatch(n -> n.getId().equals(node.getId()));

        if (!exists) {
            group.add(node);
        }

        List<Node> currentGroup = new ArrayList<>(group);

        for (Node currentNode : currentGroup) {

            if (currentNode.equals(nodeConfig.getSelf())) {
                continue;
            }
            new Thread(() -> {
                try {
                    RestTemplate rest = new RestTemplate();
                    rest.postForEntity(
                            "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/group/refresh",
                            currentGroup,
                            Void.class
                    );

                } catch (Exception e) {
                    System.out.println("Falha ao enviar para " + currentNode.getId());
                }
            }).start();
        }
    }

    public List<Node> getGroup() {
        return new ArrayList<>(group);
    }
}
