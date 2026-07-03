package chat.sda.spring.service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import chat.sda.spring.dto.AgreementMessageDTO;
import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.dto.ProposalMessageDTO;
import chat.sda.spring.model.AgreementMessage;
import chat.sda.spring.model.ProposalMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;

@Service
public class ChatService {

    private final AtomicLong rg;
    @Value("${node.id}")
    private Long processId;
    private final Map<String, ChatMessage> holdBackQueue;
    private final Map<String, ConcurrentLinkedQueue<ProposalMessage>> proposalMap;
    private final Queue<ChatMessage> deliveredMessages;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final RestTemplate rest = new RestTemplate();

    public ChatService(GroupService groupService) {
        this.rg = new AtomicLong(0);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.proposalMap = new ConcurrentHashMap<>();
        this.deliveredMessages = new ConcurrentLinkedQueue<>();
        this.groupService = groupService;
    }

    //Recebe a mensagem e envia o multicast para o grupo
    public synchronized void sendMessage(ChatMessage message){

        groupService.getGroup().forEach(node -> sendMessageToNode(node,message));

    }

    //Monta a requisição do multicast
    private synchronized void sendMessageToNode(Node node, ChatMessage message){

        try {

            ResponseEntity<ProposalMessageDTO> response = rest.postForEntity(
                    "http://" + node.getHost() + ":" + node.getPort() + "/chat/proposal",
                    message,
                    ProposalMessageDTO.class
            );
            ProposalMessage currentProposal = new ProposalMessage(response.getBody().getMessageId(), response.getBody().getSequenceNumber(), response.getBody().getProcessProposalId());
            proposalMap.computeIfAbsent(currentProposal.getMessageId(), k -> new ConcurrentLinkedQueue<>()).offer(currentProposal);
            checkAndSendAgreement(response.getBody().getMessageId());

        } catch (Exception e) {
            System.out.println("Falha ao enviar para " + node.getId());
        }

    }

    // Os peers recebem a mensagem, criam uma proposta e retorna ao processo que enviou, isto é, o sender
    public synchronized ProposalMessage createProposal(ChatMessage message){

        holdBackQueue.put(message.getId(), message);
        ProposalMessage proposalMessage = new ProposalMessage(message.getId(),rg.incrementAndGet(), processId);
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

        try {
            rest.postForEntity(
                    "http://" + node.getHost() + ":" + node.getPort() + "/chat/agreement",
                    agreement,
                    Void.class
            );
        } catch (Exception e) {
            System.out.println("Falha ao enviar para " + node.getId());
        }
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

            log.info("Mensagem Liberada - Id:{} - Conteudo:{}", message.getId(), message.getContent());

        }
    }

    public synchronized Queue<ChatMessage> getMessages(){

        return deliveredMessages;

    }


}
