package chat.sda.spring.service;

import chat.sda.spring.dto.AgreementMessageDTO;
import chat.sda.spring.dto.ProposalMessageDTO;
import chat.sda.spring.model.AgreementMessage;
import chat.sda.spring.model.ChatMessage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private final AtomicLong rg;
    private final NodeConfig nodeConfig;
    private final Map<String, ChatMessage> holdBackQueue;
    private final Map<String, ConcurrentLinkedQueue<ProposalMessage>> proposalMap;
    private final Map<String, ArrayList<Node>> messageGroups;
    private final Queue<ChatMessage> deliveredMessages;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public ChatService(GroupService groupService, NodeConfig nodeConfig) {

        this.rg = new AtomicLong(0);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.proposalMap = new ConcurrentHashMap<>();
        this.messageGroups = new ConcurrentHashMap<>();
        this.deliveredMessages = new ConcurrentLinkedQueue<>();
        this.groupService = groupService;
        this.nodeConfig = nodeConfig;
    }

    public synchronized void sendMessage(ChatMessage message) {

        message.setProcessSenderId(nodeConfig.getSelf().getId());

        ArrayList<Node> currentView = groupService.getGroup();

        messageGroups.put(message.getId(), currentView);
        log.info(
                "Multicast iniciado -> MessageId: {} | Sender: {} | ViewSize: {} | View: {}",
                message.getId(),
                nodeConfig.getSelf().getId(),
                currentView.size(),
                currentView.stream().map(Node::getId).toList()
        );

        currentView.forEach(node -> sendMessageToNode(node, message));
    }


    private synchronized void sendMessageToNode(Node node, ChatMessage message) {

        executor.submit(() -> {

            try {


                ResponseEntity<ProposalMessageDTO> response = rest.postForEntity("http://" + node.getHost() + ":" + node.getPort() + "/chat/proposal", message, ProposalMessageDTO.class);
                ProposalMessage currentProposal = new ProposalMessage(response.getBody().getMessageId(), response.getBody().getSequenceNumber(), response.getBody().getProcessProposerId());
                proposalMap.computeIfAbsent(currentProposal.getMessageId(), k -> new ConcurrentLinkedQueue<>()).offer(currentProposal);
                log.info(
                        "Proposta recebida -> MessageId: {} | Sequence: {} | ProposerId: {} | TotalRecebidas: {}/{}",
                        currentProposal.getMessageId(),
                        currentProposal.getSequenceNumber(),
                        currentProposal.getProcessProposerId(),
                        proposalMap.get(currentProposal.getMessageId()).size(),
                        messageGroups.get(currentProposal.getMessageId()).size()
                );
                checkAndSendAgreement(response.getBody().getMessageId());


            } catch (Exception e) {

                if (groupService.getLeader() != null && node.getId().equals(groupService.getLeader().getId())) {

                    groupService.initBullyVote();

                } else if (groupService.getLeader() != null) {

                    rest.postForEntity("http://" + groupService.getLeader().getHost() + ":" + groupService.getLeader().getPort() + "/group/node-failure", node, Void.class);

                    log.info("Falha reportada ao líder para o nó {}", node.getId());

                }

            }

        });

    }

    public synchronized ProposalMessage createProposal(ChatMessage message) {

        holdBackQueue.put(message.getId(), message);
        ProposalMessage proposalMessage = new ProposalMessage(message.getId(), rg.incrementAndGet(), nodeConfig.getSelf().getId());
        log.info(
                "Proposta criada -> MessageId: {} | Sequence: {} | ProposerId: {} | Node: {}",
                proposalMessage.getMessageId(),
                proposalMessage.getSequenceNumber(),
                proposalMessage.getProcessProposerId(),
                nodeConfig.getSelf().getId()
        );
        log.info(
                "Mensagem armazenada na HoldBackQueue -> MessageId: {} | SenderId: {} | Conteúdo: {}",
                message.getId(),
                message.getSenderId(),
                message.getContent()
        );

        return proposalMessage;
    }

    private void checkAndSendAgreement(String messageId) {

        ConcurrentLinkedQueue<ProposalMessage> proposals = proposalMap.get(messageId);

        if (proposals == null) {
            return;
        }


        ArrayList<Node> messageGroup = messageGroups.get(messageId);

        if (messageGroup == null) {
            return;
        }

        if (proposals.size() == messageGroup.size()) {

            log.info(
                    "Todas propostas recebidas -> MessageId: {} | Propostas: {} | ViewEsperada: {}",
                    messageId,
                    proposals.size(),
                    messageGroup.stream().map(Node::getId).toList()
            );

            ProposalMessage max = proposals.stream().max(Comparator.comparingLong(ProposalMessage::getSequenceNumber).thenComparingLong(ProposalMessage::getProcessProposerId)).orElseThrow();
            AgreementMessage agreement = new AgreementMessage(max.getMessageId(), max.getSequenceNumber(), max.getProcessProposerId());

            log.info(
                    "Agreement definido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}",
                    agreement.getMessageId(),
                    agreement.getSequenceNumber(),
                    agreement.getProcessProposerId()
            );

            messageGroup.forEach(node -> sendMessageAgreementToNode(node, agreement));

            proposalMap.remove(messageId);
            messageGroups.remove(messageId);

        }

    }

    private synchronized void sendMessageAgreementToNode(Node node, AgreementMessage agreement) {

        executor.submit(() -> {

            try {

                log.info(
                        "Enviando Agreement -> MessageId: {} | Destino: {} | SequenceFinal: {} | ProposerFinal: {}",
                        agreement.getMessageId(),
                        node.getId(),
                        agreement.getSequenceNumber(),
                        agreement.getProcessProposerId()
                );

                rest.postForEntity("http://" + node.getHost() + ":" + node.getPort() + "/chat/agreement", agreement, Void.class);

            } catch (Exception e) {

                if (groupService.getLeader() != null && node.getId().equals(groupService.getLeader().getId())) {

                    groupService.initBullyVote();

                } else if (groupService.getLeader() != null) {

                    rest.postForEntity("http://" + groupService.getLeader().getHost() + ":" + groupService.getLeader().getPort() + "/group/node-failure", node, Void.class);

                    log.info("Falha reportada ao líder para o nó {}", node.getId());

                }

            }

        });

    }

    public synchronized void receiveAgreement(AgreementMessageDTO agreement) {

        rg.updateAndGet(current -> Math.max(current, agreement.getSequenceNumber()) + 1);
        ChatMessage refreshMessage = holdBackQueue.get(agreement.getMessageId());

        if (refreshMessage == null) {
            return;
        }

        refreshMessage.setSequenceNumber(agreement.getSequenceNumber());
        refreshMessage.setProcessProposerId(agreement.getProcessProposerId());
        refreshMessage.setDeliverable(true);
        log.info(
                "Agreement recebido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}",
                agreement.getMessageId(),
                agreement.getSequenceNumber(),
                agreement.getProcessProposerId()
        );
        scheduler.schedule(this::refreshMessages, 100, TimeUnit.MILLISECONDS);

    }

    public synchronized void refreshMessages() {

        Optional<ChatMessage> next = holdBackQueue.values().stream().filter(ChatMessage::getDeliverable).min(Comparator.comparingLong(ChatMessage::getSequenceNumber).thenComparingLong(ChatMessage::getProcessProposerId));

        if (next.isPresent()) {

            ChatMessage message = next.get();

            deliveredMessages.add(message);
            holdBackQueue.remove(message.getId());

            log.info(
                    "Mensagem entregue -> MessageId: {} | Conteúdo: {} | Sequence: {} | Sender: {} | Proposer: {}",
                    message.getId(),
                    message.getContent(),
                    message.getSequenceNumber(),
                    message.getSenderId(),
                    message.getProcessProposerId()
            );

        }

    }

    public synchronized Queue<ChatMessage> getMessages() {

        return deliveredMessages;

    }

}