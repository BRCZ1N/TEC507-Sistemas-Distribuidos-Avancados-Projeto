package chat.sda.spring.service;

import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupService {

    private final Map<String,Node> group;
    private final NodeConfig nodeConfig;
    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    public GroupService(NodeConfig nodeConfig) {

        this.group = new ConcurrentHashMap<>();
        this.nodeConfig = nodeConfig;
        this.group.put(nodeConfig.getSelf().getId(), nodeConfig.getSelf());

    }

    public void join(Node node) {

        group.putIfAbsent(node.getId(), node);

        log.info("Peer Id:{} - Ip:{} - Porta:{} entrou no grupo", node.getId(), node.getHost(), node.getPort());

        for (Node currentNode : group.values()) {

            if (currentNode.equals(nodeConfig.getSelf())) {
                continue;
            }
            new Thread(() -> {
                try {
                    RestTemplate rest = new RestTemplate();
                    rest.postForEntity(
                            "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/group/refresh",
                            new ArrayList<>(group.values()),
                            Void.class
                    );

                } catch (Exception e) {
                    System.out.println("Falha ao enviar para " + currentNode.getId());
                }
            }).start();
        }
    }

    public ArrayList<Node> getGroup() {
        return new ArrayList<>(group.values());
    }
}
