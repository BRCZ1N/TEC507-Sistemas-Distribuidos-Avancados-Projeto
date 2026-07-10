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

    @Value("${simulation.delay.enabled}")
    private Boolean delay;

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

            entry = entry.trim();

            String[] parts = entry.split(":", 3);

            Long peerId = Long.valueOf(parts[0]);

            String peerHost;
            Integer peerPort = null;

            if (parts.length == 2) {

                peerHost = parts[1];

            } else {

                peerHost = parts[1];

                if (!parts[2].isBlank()) {
                    peerPort = Integer.parseInt(parts[2]);
                }
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

    public Boolean isDelayEnabled() {
        return this.delay;
    }
}