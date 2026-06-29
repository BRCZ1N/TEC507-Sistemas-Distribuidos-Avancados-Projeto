package chat.sda.spring.service;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import chat.sda.spring.model.ChatMessage;
import chat.sda.spring.model.Node;
import chat.sda.spring.model.OrderMessage;
import chat.sda.spring.utils.NodeConfig;

@Service
public class ChatService {

    private final AtomicLong rg;
    private final Map<String, ChatMessage> holdBackQueue;
    private final Map<Long, OrderMessage> orderQueue;
    private final Queue<ChatMessage> deliveredMessages;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public ChatService(NodeConfig nodeConfig, GroupService groupService) {

        this.rg = new AtomicLong(1);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.orderQueue = new ConcurrentHashMap<>();
        this.deliveredMessages = new ConcurrentLinkedQueue<>();
        this.groupService = groupService;
    }

    public synchronized void refreshMessages() {

        while (orderQueue.containsKey(rg.get())) {

            OrderMessage order = orderQueue.get(rg.get());
            ChatMessage message = holdBackQueue.get(order.getMessageId());

            if (message == null) {
                break;
            }

            ChatMessage currentTopMessage = holdBackQueue.remove(order.getMessageId());
            OrderMessage currentTopOrderMessage = orderQueue.remove(rg.get());
            log.info("Sequencia Local Atual(rg):{}", rg.get());
            log.info("Mensagem - Id:{} - SenderId:{} - Conteudo:{}", currentTopMessage.getId(), currentTopMessage.getSenderId(), currentTopMessage.getContent());
            log.info("Ordem - Message Id:{} - Sg:{}", currentTopOrderMessage.getMessageId(), currentTopOrderMessage.getSequenceNumber());
            deliveredMessages.add(message);
            log.info("Mensagem Liberada - Id:{} - SenderId:{} - Conteudo:{}", message.getId(), message.getSenderId(), message.getContent());
            rg.incrementAndGet();
            log.info("Nova Sequencia Local Atual(rg):{}", rg.get());
        }
    }

    public void multiCast(ChatMessage message){

        bMulticast(message);

    }

    public synchronized void bDeliver(ChatMessage message){

        holdBackQueue.put(message.getId(), message);
        log.info("Mensagem armazenada - Id:{} - SenderId:{} - Conteudo:{}", message.getId(), message.getSenderId(), message.getContent());
        refreshMessages();
    }

    public synchronized void orderDeliver(OrderMessage order){

        orderQueue.put(order.getSequenceNumber(),order);
        log.info("Ordem armazenada - Message Id:{} - SG:{}", order.getMessageId(), order.getSequenceNumber());
        refreshMessages();
    }

    public Queue<ChatMessage> getMessages(){

        return deliveredMessages;

    }


    public void bMulticast(ChatMessage message) {

        ArrayList<Node> currentGroup = new ArrayList<>(groupService.getGroup());

        for (Node node : currentGroup) {

            new Thread(() -> {
                try {

                    RestTemplate rest = new RestTemplate();

                    rest.postForEntity(
                            "http://" + node.getHost() + ":" + node.getPort() + "/chat/deliver",
                            message,
                            Void.class
                    );

                } catch (Exception e) {
                    System.out.println("Falha ao enviar para " + node.getId());
                }
            }).start();
        }
    }

}
