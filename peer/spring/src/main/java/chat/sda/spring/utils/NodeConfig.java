package chat.sda.spring.utils;

import chat.sda.spring.model.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class NodeConfig {

    @Value("${node.id}")
    private Long id;

    @Value("${node.host}")
    private String host;

    @Value("${node.peers}")
    private String peersRaw;

    private Node self;
    private List<Node> peers;

    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerReady(WebServerInitializedEvent event) throws UnknownHostException {

        int port = event.getWebServer().getPort();

        if (host == null || host.isBlank()) {
            host = InetAddress.getLocalHost().getHostAddress();
        }

        self = new Node(id, host, port);
        peers = parsePeers(peersRaw);
    }

    private List<Node> parsePeers(String raw) {

        List<Node> result = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(",")) {

            String[] parts = entry.trim().split(":");

            Long peerId = Long.valueOf(parts[0]);
            String peerHost = parts[1];

            Integer peerPort = null;

            if (parts.length >= 3 && !parts[2].isBlank()) {
                peerPort = Integer.parseInt(parts[2]);
            }

            result.add(new Node(peerId, peerHost, peerPort));
        }

        return result;
    }

    public Node getSelf() {
        return self;
    }

    public List<Node> getPeers() {
        return Collections.unmodifiableList(peers);
    }
}