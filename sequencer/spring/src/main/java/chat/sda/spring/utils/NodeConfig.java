package chat.sda.spring.utils;

import chat.sda.spring.model.Node;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeConfig {

    @Value("${node.id}")
    private String id;

    @Value("${node.host}")
    private String host;

    @Value("${server.port}")
    private int port;

    private Node self;

    @PostConstruct
    public void init() {
        this.self = new Node(id, host, port);
    }

    public Node getSelf() {
        return self;
    }
}