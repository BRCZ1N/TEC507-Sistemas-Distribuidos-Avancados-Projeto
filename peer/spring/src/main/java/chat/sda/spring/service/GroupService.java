package chat.sda.spring.service;
import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final Node nodeSequencer;
    private final NodeConfig nodeConfig;
    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    public GroupService(NodeConfig nodeConfig, @Value("${sequencer.id}") String sequencerId, @Value("${sequencer.host}")  String sequencerHost, @Value("${sequencer.port}") int sequencerPort) {

        this.group = new ConcurrentHashMap<>();
        this.nodeSequencer = new Node(sequencerId, sequencerHost, sequencerPort);
        this.nodeConfig = nodeConfig;
    }

    public ArrayList<Node> getGroup() {
        return new ArrayList<>(group.values());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {

        new Thread(() -> {

            while (true) {
                try {
                    RestTemplate rest = new RestTemplate();

                    rest.postForEntity(
                            "http://" + nodeSequencer.getHost() + ":" + nodeSequencer.getPort() + "/group/join",
                            nodeConfig.getSelf(),
                            Void.class
                    );

                    log.info("Enviando mensagem para entrada no grupo - Peer Id:{} - Ip:{} - Porta:{}", nodeConfig.getSelf().getId(), nodeConfig.getSelf().getHost(), nodeConfig.getSelf().getPort());
                    break;
                } catch (Exception e) {
                    log.warn("Sequencer não disponível, tentando novamente...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }
            }

        }).start();
    }

    public void join() {

        new Thread(() -> {
            RestTemplate rest = new RestTemplate();

            rest.postForEntity(
                    "http://" + nodeSequencer.getHost() + ":" + nodeSequencer.getPort() + "/group/join",
                    nodeConfig.getSelf(),
                    Void.class
            );

            log.info("Enviando mensagem para entrada - Peer Id:{} - Ip:{} - Porta:{}", nodeConfig.getSelf().getId(), nodeConfig.getSelf().getHost(), nodeConfig.getSelf().getPort());
        }).start();
    }

    public void refreshGroup(ArrayList<Node> newGroup) {

        for (Node node : newGroup) {

            group.putIfAbsent(node.getId(), node);

        }
    }

}
