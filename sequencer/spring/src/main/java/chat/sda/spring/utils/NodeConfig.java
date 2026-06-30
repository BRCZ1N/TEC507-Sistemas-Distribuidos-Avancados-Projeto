package chat.sda.spring.utils;

import chat.sda.spring.model.Node;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class NodeConfig {

    @Value("${node.id}")
    private String id;

    @Value("${node.host}")
    private String host;

    @Value("${server.port}")
    private int port;

    private Node self;

    public NodeConfig(@Value("${node.id}") String id, @Value("${node.host}") String host, @Value("${server.port}") int port) throws UnknownHostException {

        String resolvedHost = (host == null || host.isBlank()) ? InetAddress.getLocalHost().getHostAddress() : host;

        this.self = new Node(id, resolvedHost, port);
    }

    public Node getSelf() {
        return self;
    }
}