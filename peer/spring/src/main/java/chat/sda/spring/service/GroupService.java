package chat.sda.spring.service;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;
import chat.sda.spring.utils.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
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
    private final AtomicBoolean electionRunning = new AtomicBoolean(false);
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

            String hostBase = nodeConfig.getHostBase();

            int firstPort = 60000;
            int lastPort = 60050;

            for (int port = firstPort; port <= lastPort; port++) {

                if (port == nodeConfig.getSelf().getPort() && nodeConfig.getHostBase().equals(nodeConfig.getSelf().getHost())) continue;

                try {

                    ResponseEntity<Node> response = rest.getForEntity(
                            hostBase + port + "/group/leader",
                            Node.class
                    );

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        this.currentLeader = response.getBody();
                        log.info("Nó ativo encontrado na porta {}. Ele informou que o líder é o Nó {}", port, currentLeader.getId());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Porta não alocada ou não encontrada");
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

    private void removeNode(Long idNode) {

        if (currentLeader == null || !nodeConfig.getSelf().getId().equals(currentLeader.getId())) {
            return;
        }

        group.remove(idNode);

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
                    log.warn("Falha ao enviar para o Id:{}",currentNode.getId());
                }
            });
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
                    log.warn("Falha ao enviar para o Id:{}", currentNode.getId());
                }
            });
        }
    }

    public synchronized void refreshGroup(ArrayList<Node> newGroup) {

        group.clear();

        for (Node node : newGroup) {

            group.put(node.getId(), node);

        }

    }

    public void initBullyVote() {

        if (!electionRunning.compareAndSet(false, true)) {
            log.info("[BULLY] Eleição já está em andamento.");
            return;
        }

        executor.submit(() -> {

            try {
                log.info("[BULLY] Iniciando eleição ... Meu ID é {}", nodeConfig.getSelf().getId());

                receivedOk.set(false);

                boolean higherNodeFound = false;

                for (Node remoteNode : group.values()) {

                    if (remoteNode.getId() > nodeConfig.getSelf().getId()) {

                        higherNodeFound = true;

                        try {
                            log.info(
                                    "[BULLY] Desafiando Nó Maior -> ID {} na porta {}",
                                    remoteNode.getId(),
                                    remoteNode.getPort()
                            );

                            rest.postForEntity(
                                    "http://"
                                            + remoteNode.getHost()
                                            + ":"
                                            + remoteNode.getPort()
                                            + "/group/election?senderNode="
                                            + nodeConfig.getSelf().getId(),
                                    null,
                                    Void.class
                            );

                            receivedOk.set(true);

                        } catch (Exception e) {

                            log.warn(
                                    "[BULLY] Nó {} não respondeu ao ELECTION.",
                                    remoteNode.getId()
                            );
                        }
                    }
                }

                if (higherNodeFound) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}

                }

                if (!receivedOk.get()) {

                    log.info("[BULLY] Nenhum nó maior respondeu. Eu sou o novo Líder!");

                    this.currentLeader = nodeConfig.getSelf();
                    this.group.put(currentLeader.getId(), currentLeader);

                    announceCoordinator();
                }

            } finally {

                electionRunning.set(false);

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
                    try {
                        log.warn("Falha ao anunciar coordenação para o nó {}. Nova tentativa...", currentNode.getId());
                        Thread.sleep(500);
                        rest.postForEntity(
                                "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/group/coordinator",
                                nodeConfig.getSelf(),
                                Void.class
                        );
                    } catch (Exception e1) {
                        log.warn("Nó {} removido do grupo após duas falhas.", currentNode.getId());
                        reportNode(currentNode);
                    }
                }
            });
        }
    }

    public void reportNode(Node node){

        executor.submit(() -> {
           try{
               rest.getForEntity(
                       "http://" + node.getHost() + ":" + node.getPort() + "/group/heartbeat",
                       Void.class
               );
               log.info("Nó {} respondeu ao heartbeat. Mantendo no grupo.", node.getId());
           }catch (Exception e){
               log.warn("Nó {} confirmado como falho.", node.getId());
               removeNode(node.getId());
           }
        });

    }



    @Scheduled(fixedDelay = 2000)
    public void verifyLeader() {

        Node leader = getLeader();

        if (leader == null || leader.getId().equals(nodeConfig.getSelf().getId())) {
            return;
        }

        log.info("Lider local: Id: {} - Ip: {} - Porta: {}", leader.getId(),leader.getHost(),leader.getPort());
        log.info("Verificação de líder");

        try {

            rest.getForEntity(
                    "http://" + leader.getHost() + ":" + leader.getPort() + "/group/heartbeat",
                    Void.class
            );
            log.info("Líder ativo");

        } catch (Exception e) {
            log.info("Líder não encontrado");
            initBullyVote();
        }
    }


}
