package chat.sda.spring.service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import chat.sda.spring.dto.AgreementMessageDTO;
import chat.sda.spring.dto.ProposalMessageDTO;
import chat.sda.spring.model.AgreementMessage;
import chat.sda.spring.model.ProposalMessage;
import chat.sda.spring.utils.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;

@Service
public class ChatService {

    private final AtomicLong rg;
    private final NodeConfig nodeConfig;
    private final Map<String, ChatMessage> holdBackQueue;
    private final Map<String, ConcurrentLinkedQueue<ProposalMessage>> proposalMap;
    private final Queue<ChatMessage> deliveredMessages;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatService(GroupService groupService, NodeConfig nodeConfig) {
        this.rg = new AtomicLong(0);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.proposalMap = new ConcurrentHashMap<>();
        this.deliveredMessages = new ConcurrentLinkedQueue<>();
        this.groupService = groupService;
        this.nodeConfig = nodeConfig;
    }

    //Recebe a mensagem e envia o multicast para o grupo
    public synchronized void sendMessage(ChatMessage message){
        message.setProcessSenderId(nodeConfig.getSelf().getId());
        groupService.getGroup().forEach(node -> sendMessageToNode(node,message));

    }

    //Monta a requisição do multicast
    private synchronized void sendMessageToNode(Node node, ChatMessage message){
        executor.submit(() -> {
            try {

                ResponseEntity<ProposalMessageDTO> response = rest.postForEntity(
                        "http://" + node.getHost() + ":" + node.getPort() + "/chat/proposal",
                        message,
                        ProposalMessageDTO.class
                );
                ProposalMessage currentProposal = new ProposalMessage(response.getBody().getMessageId(), response.getBody().getSequenceNumber(), response.getBody().getProcessProposerId());
                proposalMap.computeIfAbsent(currentProposal.getMessageId(), k -> new ConcurrentLinkedQueue<>()).offer(currentProposal);
                log.info(
                        "Proposal recebida -> messageId: {}, sequence: {}, proposerId: {}",
                        currentProposal.getMessageId(),
                        currentProposal.getSequenceNumber(),
                        currentProposal.getProcessProposerId()
                );
                checkAndSendAgreement(response.getBody().getMessageId());

            } catch (Exception e) {
                if (node == groupService.getLeader()){

                    groupService.initBullyVote();

                }
                System.out.println("Falha ao enviar a proposta para " + node.getId());
            }
        });
    }

    // Os peers recebem a mensagem, criam uma proposta e retorna ao processo que enviou, isto é, o sender
    public synchronized ProposalMessage createProposal(ChatMessage message){

        holdBackQueue.put(message.getId(), message);
        ProposalMessage proposalMessage = new ProposalMessage(message.getId(),rg.incrementAndGet(), nodeConfig.getSelf().getId());
        log.info(
                "Proposal enviada -> messageId: {}, sequence: {}, proposerId: {}",
                proposalMessage.getMessageId(),
                proposalMessage.getSequenceNumber(),
                proposalMessage.getProcessProposerId()
        );
        log.info("Mensagem armazenada - Id:{} - SenderId:{} - Conteudo:{}", message.getId(), message.getSenderId(), message.getContent());

        return proposalMessage;
    }

    private void checkAndSendAgreement(String messageId) {

        ConcurrentLinkedQueue<ProposalMessage> proposals = proposalMap.get(messageId);

        if (proposals == null) return;

        if (proposals.size() == groupService.getGroup().size()) {

            ProposalMessage max = proposals.stream().max(Comparator.comparingLong(ProposalMessage::getSequenceNumber).thenComparingLong(ProposalMessage::getProcessProposerId)).orElseThrow();

            AgreementMessage agreement = new AgreementMessage(max.getMessageId(), max.getSequenceNumber());

            groupService.getGroup().forEach(node -> sendMessageAgreementToNode(node, agreement));

            proposalMap.remove(messageId);
        }
    }

    //Monta a requisição do multicast
    private synchronized void sendMessageAgreementToNode(Node node, AgreementMessage agreement){
        executor.submit(() -> {
            try {
                rest.postForEntity(
                        "http://" + node.getHost() + ":" + node.getPort() + "/chat/agreement",
                        agreement,
                        Void.class
                );
            } catch (Exception e) {
                if (node == groupService.getLeader()){

                    groupService.initBullyVote();

                }
                System.out.println("Falha ao enviar o acordo para " + node.getId());
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
        refreshMessage.setDeliverable(true);
        refreshMessages();
    }


    public synchronized void refreshMessages() {

        Optional<ChatMessage> next = holdBackQueue.values().stream().filter(ChatMessage::getDeliverable).min(Comparator.comparingLong(ChatMessage::getSequenceNumber).thenComparingLong(ChatMessage::getProcessProposalId));

        if (next.isPresent()){

            ChatMessage message = next.get();

            deliveredMessages.add(message);
            holdBackQueue.remove(message.getId());

            log.info(
                    "Mensagem Liberada -> Id: {} | Conteúdo: {} | Sequência: {} | SenderId: {} | ProcessSenderId: {} | ProposerId: {} | Deliverable: {}",
                    message.getId(),
                    message.getContent(),
                    message.getSequenceNumber(),
                    message.getSenderId(),
                    message.getProcessSenderId(),
                    message.getProcessProposalId(),
                    message.getDeliverable()
            );

        }
    }

    public synchronized Queue<ChatMessage> getMessages(){

        return deliveredMessages;

    }


}
