package chat.sda.spring.service;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
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

            holdBackQueue.remove(order.getMessageId());
            orderQueue.remove(rg.get());
            deliveredMessages.add(message);

            rg.incrementAndGet();
        }
    }

    public void multiCast(ChatMessage message){

        bMulticast(message);

    }

    public synchronized void bDeliver(ChatMessage message){

        holdBackQueue.put(message.getId(), message);
        refreshMessages();

    }

    public synchronized void orderDeliver(OrderMessage order){

        orderQueue.put(order.getSequenceNumber(),order);
        refreshMessages();

    }

    public Queue<ChatMessage> getMessages(){

        if(!deliveredMessages.isEmpty()){

            return deliveredMessages;

        }

        return null;

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
