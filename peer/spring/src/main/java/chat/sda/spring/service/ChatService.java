package chat.sda.spring.service;

import chat.sda.spring.dto.AgreementMessageDTO;
import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.dto.ProposalMessageDTO;
import chat.sda.spring.model.AgreementMessage;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;
import chat.sda.spring.model.ProposalMessage;
import chat.sda.spring.utils.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ChatService {

    private final AtomicLong sequence;
    private final NodeConfig nodeConfig;
    private final Map<String, ChatMessage> holdBackQueue;
    private final Map<String, ConcurrentLinkedQueue<ProposalMessage>> proposalMap;
    private final Queue<ChatMessage> deliveredMessages;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> timeoutTasks;

    public ChatService(NodeConfig nodeConfig) {
        this.sequence = new AtomicLong(0);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.proposalMap = new ConcurrentHashMap<>();
        this.deliveredMessages = new ConcurrentLinkedQueue<>();
        this.nodeConfig = nodeConfig;
        this.timeoutTasks = new ConcurrentHashMap<>();
    }

    public synchronized void sendMessage(ChatMessage message, Long artificialDelay) {

        message.setProcessSenderId(nodeConfig.getSelf().getId());

        List<Node> group = nodeConfig.getPeers();

        proposalMap.put(
                message.getId(),
                new ConcurrentLinkedQueue<>()
        );

        scheduleTimeout(message.getId(), group.size());

        log.info(
                "Multicast iniciado -> MessageId: {} | Sender: {} | ViewSize: {} | View: {}",
                message.getId(),
                nodeConfig.getSelf().getId(),
                group.size(),
                group.stream().map(Node::getId).toList()
        );

        group.forEach(node -> sendMessageToNode(node, message, group.size(),artificialDelay));
    }

    private void sendMessageToNode(Node node, ChatMessage message, int groupSize, Long artificialDelay) {

        executor.submit(() -> {
            try {
                ChatMessageDTO dto = new ChatMessageDTO(message.getSenderId(),message.getId(),message.getContent(),message.getSequenceNumber(),message.getProcessSenderId(),message.getProcessProposerId(),message.getDeliverable(),artificialDelay);
                ResponseEntity<ProposalMessageDTO> response = rest.postForEntity(
                        buildBaseUrl(node) + "/chat/proposal",
                        dto,
                        ProposalMessageDTO.class
                );

                ProposalMessage currentProposal = new ProposalMessage(
                        response.getBody().getMessageId(),
                        response.getBody().getSequenceNumber(),
                        response.getBody().getProcessProposerId()
                );

                proposalMap.computeIfAbsent(currentProposal.getMessageId(), k -> new ConcurrentLinkedQueue<>())
                        .offer(currentProposal);

                log.info(
                        "Proposta recebida -> MessageId: {} | Sequence: {} | ProposerId: {} | TotalRecebidas: {}/{}",
                        currentProposal.getMessageId(),
                        currentProposal.getSequenceNumber(),
                        currentProposal.getProcessProposerId(),
                        proposalMap.get(currentProposal.getMessageId()).size(),
                        groupSize
                );

                checkAndSendAgreement(currentProposal.getMessageId(), groupSize);

            } catch (Exception e) {
                log.error("Falha ao contatar nó {} para MessageId: {} -> {}", node.getId(), message.getId(), e.getMessage());
            }
        });
    }

    private synchronized void checkAndSendAgreement(String messageId, int groupSize) {

        ConcurrentLinkedQueue<ProposalMessage> proposals = proposalMap.get(messageId);
        if (proposals == null) {
            return;
        }

        if (proposals.size() >= groupSize) {

            log.info(
                    "Todas propostas recebidas -> MessageId: {} | Propostas: {} | ViewEsperada: {}",
                    messageId, proposals.size(), groupSize
            );

            ProposalMessage max = proposals.stream()
                    .max(Comparator.comparingLong(ProposalMessage::getSequenceNumber)
                            .thenComparingLong(ProposalMessage::getProcessProposerId))
                    .orElseThrow();

            AgreementMessage agreement = new AgreementMessage(
                    max.getMessageId(), max.getSequenceNumber(), max.getProcessProposerId()
            );

            log.info(
                    "Agreement definido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}",
                    agreement.getMessageId(), agreement.getSequenceNumber(), agreement.getProcessProposerId()
            );

            nodeConfig.getPeers().forEach(node -> sendMessageAgreementToNode(node, agreement));

            ScheduledFuture<?> timeout = timeoutTasks.remove(messageId);

            if (timeout != null) {
                timeout.cancel(false);
            }

            proposalMap.remove(messageId);
        }
    }

    private void scheduleTimeout(String messageId, int groupSize) {

        ScheduledFuture<?> future = scheduler.schedule(() -> {

            synchronized (this) {

                ConcurrentLinkedQueue<ProposalMessage> proposals =
                        proposalMap.get(messageId);

                if (proposals == null) {
                    return;
                }

                if (proposals.size() < groupSize) {

                    log.warn(
                            "Timeout da mensagem {} -> Recebidas {}/{} propostas. Enviando abort.",
                            messageId,
                            proposals.size(),
                            groupSize
                    );

                    nodeConfig.getPeers()
                            .forEach(node -> sendAbortToNode(node, messageId));


                    proposalMap.remove(messageId);
                }
            }

        }, 3, TimeUnit.SECONDS);


        timeoutTasks.put(messageId, future);
    }

    private void sendMessageAgreementToNode(Node node, AgreementMessage agreement) {

        executor.submit(() -> {
            try {

                log.info(
                        "Enviando Agreement -> MessageId: {} | Destino: {} | SequenceFinal: {} | ProposerFinal: {}",
                        agreement.getMessageId(), node.getId(), agreement.getSequenceNumber(), agreement.getProcessProposerId()
                );

                rest.postForEntity(
                        buildBaseUrl(node) + "/chat/agreement",
                        agreement,
                        Void.class
                );

            } catch (Exception e) {
                log.error("Falha ao enviar agreement ao nó {} para MessageId: {} -> {}", node.getId(), agreement.getMessageId(), e.getMessage());
            }
        });
    }

    public synchronized ProposalMessage createProposal(ChatMessage message, Long artificialDelay) {

        if (nodeConfig.isDelayEnabled() && artificialDelay != null) {
            try {
                Thread.sleep(artificialDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrompida durante atraso artificial");
            }
        }
        long propNum = sequence.incrementAndGet();

        ProposalMessage proposalMessage = new ProposalMessage(
                message.getId(), propNum, nodeConfig.getSelf().getId()
        );

        message.setSequenceNumber(propNum);
        message.setProcessProposerId(nodeConfig.getSelf().getId());
        message.setDeliverable(false);

        holdBackQueue.put(message.getId(), message);

        log.info(
                "Proposta criada -> MessageId: {} | Sequence: {} | ProposerId: {} | Node: {}",
                proposalMessage.getMessageId(), proposalMessage.getSequenceNumber(),
                proposalMessage.getProcessProposerId(), nodeConfig.getSelf().getId()
        );
        log.info(
                "Mensagem armazenada na HoldBackQueue -> MessageId: {} | SenderId: {} | Conteúdo: {}",
                message.getId(), message.getSenderId(), message.getContent()
        );

        return proposalMessage;
    }

    public synchronized void receiveAgreement(AgreementMessageDTO agreement) {

        sequence.updateAndGet(current -> Math.max(current, agreement.getSequenceNumber()));

        ChatMessage refreshMessage = holdBackQueue.get(agreement.getMessageId());
        if (refreshMessage == null) {
            log.warn("Agreement recebido para mensagem desconhecida -> MessageId: {}", agreement.getMessageId());
            return;
        }

        refreshMessage.setSequenceNumber(agreement.getSequenceNumber());
        refreshMessage.setProcessProposerId(agreement.getProcessProposerId());
        refreshMessage.setDeliverable(true);

        log.info(
                "Agreement recebido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}",
                agreement.getMessageId(), agreement.getSequenceNumber(), agreement.getProcessProposerId()
        );

        refreshMessages();
    }

    public synchronized void refreshMessages() {

        while (true) {

            Optional<ChatMessage> headOpt = holdBackQueue.values().stream()
                    .min(Comparator.comparingLong(ChatMessage::getSequenceNumber)
                            .thenComparingLong(ChatMessage::getProcessProposerId));

            if (headOpt.isEmpty()) {
                break;
            }

            ChatMessage head = headOpt.get();

            if (!head.getDeliverable()) {
                break;
            }

            holdBackQueue.remove(head.getId());
            deliveredMessages.add(head);

            log.info(
                    "Mensagem entregue -> MessageId: {} | Conteúdo: {} | Sequence: {} | Sender: {} | Proposer: {}",
                    head.getId(), head.getContent(), head.getSequenceNumber(),
                    head.getSenderId(), head.getProcessProposerId()
            );
        }
    }

    public List<Node> getPeers(){

        return this.nodeConfig.getPeers();

    }

    public synchronized Queue<ChatMessage> getMessages() {
        return deliveredMessages;
    }

    private String buildBaseUrl(Node node) {

        if (node.getHost().contains("onrender.com")) {
            return "https://" + node.getHost();
        }

        if (node.getPort() == null) {
            return "https://" + node.getHost();
        }

        return "http://" + node.getHost() + ":" + node.getPort();
    }

    public synchronized void receiveAbort(String messageId) {

        ChatMessage removed = holdBackQueue.remove(messageId);

        if (removed != null) {

            log.warn("Mensagem abortada removida da HoldBackQueue -> MessageId: {}", messageId);

        } else {

            log.warn("Abort recebido mas mensagem não encontrada -> MessageId: {}", messageId);
        }
    }

    private void sendAbortToNode(Node node, String messageId) {

        executor.submit(() -> {

            try {

                rest.postForEntity(
                        buildBaseUrl(node)
                                + "/chat/abort/"
                                + messageId,
                        null,
                        Void.class
                );

            } catch(Exception e) {

                log.error(
                        "Falha ao enviar abort para {}",
                        node.getId()
                );
            }
        });
    }

}