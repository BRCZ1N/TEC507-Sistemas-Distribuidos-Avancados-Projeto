package chat.sda.spring.service;
import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GroupService {

    private final Map<Long,Node> group;
    private Node currentLeader;
    private final NodeConfig nodeConfig;
    private static final Logger log = LoggerFactory.getLogger(GroupService.class);
    private final RestTemplate rest;
    private final AtomicBoolean receivedOk;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GroupService(NodeConfig nodeConfig) {
        this.rest = new RestTemplate();
        this.group = new ConcurrentHashMap<>();
        this.nodeConfig = nodeConfig;
        this.receivedOk = new AtomicBoolean(false);
    }

    public ArrayList<Node> getGroup() {
        return new ArrayList<>(group.values());
    }

    public Node getLeader(){
        return currentLeader;
    }

    public void refreshCoordinator(Node newCoordinator){
        this.currentLeader = newCoordinator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        executor.submit(() -> {
            log.info("Iniciando varredura no intervalo de portas para descobrir o líder...");

            String hostBase = "http://localhost:";

            int firstPort = 60000;
            int lastPort = 60050;

            for (int port = firstPort; port <= lastPort; port++) {

                if (port == nodeConfig.getSelf().getPort()) continue;

                try {

                    ResponseEntity<Node> response = rest.getForEntity(
                            hostBase + port + "/group/leader",
                            Node.class
                    );

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        this.currentLeader = response.getBody();
                        log.info("Nó ativo encontrado na porta {}. Ele informou que o líder é o Nó {}", port, currentLeader);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Porta não alocada ou não encontrada");
                }
            }
            if (this.currentLeader != null) {
                joinRegisterInLeader();
            } else {
                log.warn("Nenhum líder respondeu no intervalo de portas. Iniciando eleição...");
                initBullyVote();
            }

        });
    }

    private void joinRegisterInLeader() {

        String urlLeader = "http://" + currentLeader.getHost() + ":" + currentLeader.getPort();

        try {
            log.info("Se registrando no líder (Nó {} em {})", currentLeader.getId(), urlLeader);

            rest.postForEntity(
                    urlLeader + "/group/join",
                    nodeConfig.getSelf(),
                    Void.class
            );
        } catch (Exception e) {
            log.error("Líder caiu ao tentar o registro. Disparando eleição.");
            initBullyVote();
        }
    }

    public void join(Node node) {

        group.putIfAbsent(node.getId(), node);

        log.info("Peer Id:{} - Ip:{} - Porta:{} entrou no grupo", node.getId(), node.getHost(), node.getPort());

        for (Node currentNode : group.values()) {

            if (currentNode.equals(nodeConfig.getSelf())) {
                continue;
            }
            executor.submit(() -> {
                try {
                    rest.postForEntity(
                            "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/group/refresh",
                            new ArrayList<>(group.values()),
                            Void.class
                    );

                } catch (Exception e) {
                    System.out.println("Falha ao enviar para " + currentNode.getId());
                }
            });
        }
    }

    public void refreshGroup(ArrayList<Node> newGroup) {

        for (Node node : newGroup) {

            group.putIfAbsent(node.getId(), node);

        }
    }

    public void initBullyVote() {

        executor.submit(() -> {
            log.info("[BULLY] Iniciando eleição ... Meu ID é {}", nodeConfig.getSelf().getId());
            receivedOk.set(false);

            for (Node remoteNode : group.values()) {

                if (remoteNode.getId() > nodeConfig.getSelf().getId()) {
                    try {
                        log.info("[BULLY] Desafiando Nó Maior -> ID {} na porta {}", remoteNode.getId(), remoteNode.getPort());

                        rest.postForEntity(
                                "http://" + remoteNode.getHost() + ":" + remoteNode.getPort() + "/group/election?senderNode="+nodeConfig.getSelf().getId(),
                                null,
                                Void.class
                        );
                        receivedOk.set(true);
                    } catch (Exception e) {
                        log.warn("[BULLY] Nó {} na porta {} não respondeu ao ELECTION (deve ter caído).", remoteNode.getId(), remoteNode.getPort());
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            if (!receivedOk.get()) {
                log.info("[BULLY] Nenhum nó maior respondeu. Eu sou o novo Líder!");
                this.currentLeader = nodeConfig.getSelf();
                this.group.put(currentLeader.getId(), currentLeader);

                announceCoordinator();
            }
        });
    }

    private void announceCoordinator() {

        for (Node currentNode : group.values()) {

            if (currentNode.equals(nodeConfig.getSelf())) {
                    continue;
            }
            executor.submit(() -> {
                try {
                    rest.postForEntity(
                            "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/group/coordinator",
                            nodeConfig.getSelf(),
                            Void.class
                    );

                } catch (Exception e) {
                        System.out.println("Falha ao anunciar coordenação para " + currentNode.getId());
                }
            });
        }
    }

}
