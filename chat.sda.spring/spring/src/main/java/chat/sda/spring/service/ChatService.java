package com.chat;

import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private AtomicLong rg;
    private Map<String,ChatMessage> holdBackQueue = new ConcurrentHashMap<>();
    private Map<Long,OrderMessage> orderQueue = new ConcurrentHashMap<>();
    private final Queue<ChatMessage> deliveredMessages = new ConcurrentLinkedQueue<>();

    public ChatService() {

        this.rg = new AtomicLong(1);
        this.holdBackQueue = new ConcurrentHashMap<>();
        this.orderQueue = new ConcurrentHashMap<>();
    }

    @Scheduled(fixedDelay = 5)
    public void refreshMessages() {

        OrderMessage order = orderQueue.get(rg.get());

        if (order == null) return;

        ChatMessage message = holdBackQueue.get(order.getMessageId());

        if (message == null) return;

        holdBackQueue.remove(order.getMessageId());
        orderQueue.remove(order.getSequenceNumber());

        deliveredMessages.add(message);

        rg.incrementAndGet();
    }

    public void chatDeliver(ChatMessage message){

        holdBackQueue.put(message.getId(), chatMessage);

    }

    public void orderDeliver(OrderMessage order){

        orderQueue.put(order.getSequenceNumber(),order);

    }


    public void bMulticast(ChatMessage message){

        for(NodeConfig process: NodeConfig.values()){

            if(!processConfig.equals(process)){

                sendPool.submit(() -> {

                    try(Socket socket = new Socket(process.getHost(), process.getPort())){

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                        out.writeObject(message);
                        out.flush();

                    } catch (UnknownHostException e) {

                        throw new RuntimeException(e);

                    } catch (IOException e) {

                        System.out.println("Não foi possível conectar ao processo destino.");

                    }

                });

            }

        }

    }

}
