package chat.sda.spring.utils;

import chat.sda.spring.model.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class NodeConfig {

    @Value("${node.id}")
    private Long id;

    @Value("${node.host}")
    private String host;

    private Node self;

    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerReady(WebServerInitializedEvent event) throws UnknownHostException {
        
        int port = event.getWebServer().getPort();

        if (host == null || host.isBlank()) {
            host = InetAddress.getLocalHost().getHostAddress();
        }

        self = new Node(id, host, port);
    }

    public Node getSelf() {
        return self;
    }
}