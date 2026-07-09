package chat.sda.spring.service;

import chat.sda.spring.dto.AgreementMessageDTO;
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
    private final Map<String, ArrayList<Node>> messageGroups;
    private final Queue<ChatMessage> deliveredMessages;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatService(GroupService groupService, NodeConfig nodeConfig) {
        this.sequence = new AtomicLong(0);
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

        log.info("Multicast iniciado -> MessageId: {} | Sender: {} | ViewSize: {} | View: {}", message.getId(), nodeConfig.getSelf().getId(), currentView.size(), currentView.stream().map(Node::getId).toList());

        currentView.forEach(node -> sendMessageToNode(node, message));
    }

    private void sendMessageToNode(Node node, ChatMessage message) {

        executor.submit(() -> {
            try {

                ResponseEntity<ProposalMessageDTO> response = rest.postForEntity("http://" + node.getHost() + ":" + node.getPort() + "/chat/proposal", message, ProposalMessageDTO.class);

                ProposalMessage currentProposal = new ProposalMessage(response.getBody().getMessageId(), response.getBody().getSequenceNumber(), response.getBody().getProcessProposerId());

                proposalMap.computeIfAbsent(currentProposal.getMessageId(), k -> new ConcurrentLinkedQueue<>()).offer(currentProposal);

                log.info("Proposta recebida -> MessageId: {} | Sequence: {} | ProposerId: {} | TotalRecebidas: {}/{}", currentProposal.getMessageId(), currentProposal.getSequenceNumber(), currentProposal.getProcessProposerId(), proposalMap.get(currentProposal.getMessageId()).size(), messageGroups.get(currentProposal.getMessageId()).size());

                checkAndSendAgreement(currentProposal.getMessageId());

            } catch (Exception e) {
                handleNodeFailure(node);
            }
        });
    }

    private void handleNodeFailure(Node node) {
        if (groupService.getLeader() != null && node.getId().equals(groupService.getLeader().getId())) {
            groupService.initBullyVote();
        } else if (groupService.getLeader() != null) {
            rest.postForEntity("http://" + groupService.getLeader().getHost() + ":" + groupService.getLeader().getPort() + "/group/node-failure", node, Void.class);
            log.info("Falha reportada ao líder para o nó {}", node.getId());
        }
    }

    private synchronized void checkAndSendAgreement(String messageId) {

        ConcurrentLinkedQueue<ProposalMessage> proposals = proposalMap.get(messageId);
        if (proposals == null) {
            return;
        }

        ArrayList<Node> messageGroup = messageGroups.get(messageId);
        if (messageGroup == null) {
            return;
        }

        if (proposals.size() == messageGroup.size()) {

            log.info("Todas propostas recebidas -> MessageId: {} | Propostas: {} | ViewEsperada: {}", messageId, proposals.size(), messageGroup.stream().map(Node::getId).toList());

            ProposalMessage max = proposals.stream().max(Comparator.comparingLong(ProposalMessage::getSequenceNumber).thenComparingLong(ProposalMessage::getProcessProposerId)).orElseThrow();

            AgreementMessage agreement = new AgreementMessage(max.getMessageId(), max.getSequenceNumber(), max.getProcessProposerId());

            log.info("Agreement definido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}", agreement.getMessageId(), agreement.getSequenceNumber(), agreement.getProcessProposerId());

            messageGroup.forEach(node -> sendMessageAgreementToNode(node, agreement));

            proposalMap.remove(messageId);
            messageGroups.remove(messageId);
        }
    }

    private void sendMessageAgreementToNode(Node node, AgreementMessage agreement) {

        executor.submit(() -> {
            try {

                log.info("Enviando Agreement -> MessageId: {} | Destino: {} | SequenceFinal: {} | ProposerFinal: {}", agreement.getMessageId(), node.getId(), agreement.getSequenceNumber(), agreement.getProcessProposerId());

                rest.postForEntity("http://" + node.getHost() + ":" + node.getPort() + "/chat/agreement", agreement, Void.class);

            } catch (Exception e) {
                handleNodeFailure(node);
            }
        });
    }

    public synchronized ProposalMessage createProposal(ChatMessage message) {

        long propNum = sequence.incrementAndGet();

        ProposalMessage proposalMessage = new ProposalMessage(message.getId(), propNum, nodeConfig.getSelf().getId());

        message.setSequenceNumber(propNum);
        message.setProcessProposerId(nodeConfig.getSelf().getId());

        holdBackQueue.put(message.getId(), message);

        log.info("Proposta criada -> MessageId: {} | Sequence: {} | ProposerId: {} | Node: {}", proposalMessage.getMessageId(), proposalMessage.getSequenceNumber(), proposalMessage.getProcessProposerId(), nodeConfig.getSelf().getId());
        log.info("Mensagem armazenada na HoldBackQueue -> MessageId: {} | SenderId: {} | Conteúdo: {}", message.getId(), message.getSenderId(), message.getContent());

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

        log.info("Agreement recebido -> MessageId: {} | SequenceFinal: {} | ProposerFinal: {}", agreement.getMessageId(), agreement.getSequenceNumber(), agreement.getProcessProposerId());

        refreshMessages();
    }

    public synchronized void refreshMessages() {

        while (true) {

            Optional<ChatMessage> headOpt = holdBackQueue.values().stream().min(Comparator.comparingLong(ChatMessage::getSequenceNumber).thenComparingLong(ChatMessage::getProcessProposerId));

            if (headOpt.isEmpty()) {
                break;
            }

            ChatMessage head = headOpt.get();

            if (!head.getDeliverable()) {
                break;
            }

            holdBackQueue.remove(head.getId());
            deliveredMessages.add(head);

            log.info("Mensagem entregue -> MessageId: {} | Conteúdo: {} | Sequence: {} | Sender: {} | Proposer: {}", head.getId(), head.getContent(), head.getSequenceNumber(), head.getSenderId(), head.getProcessProposerId());
        }
    }

    public synchronized Queue<ChatMessage> getMessages() {
        return deliveredMessages;
    }
}
