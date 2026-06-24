package com.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

class ProcessNode extends Thread {

    private NodeConfig processConfig;
    private ServerSocket server;
    private AtomicLong rg;
    private Map<String,ChatMessage> holdBackQueue = new ConcurrentHashMap<>();
    private Map<Long,OrderMessage> orderQueue = new ConcurrentHashMap<>();
    private final Scanner scan = new Scanner(System.in);
    private ExecutorService sendPool = Executors.newFixedThreadPool(3);
    private ExecutorService receivePool = Executors.newFixedThreadPool(3);

    public ProcessNode(NodeConfig process) throws IOException {

        this.server = new ServerSocket(process.getPort());
        this.rg = new AtomicLong(1);
        this.processConfig = process;
        start();

    }

    public void refreshMessages(){

        while(true){

            if (!orderQueue.isEmpty()){

                OrderMessage order = orderQueue.get(rg.get());

                if(order != null){

                    ChatMessage chatMessage = holdBackQueue.get(order.getMessageId());

                    if(chatMessage != null){

                        holdBackQueue.remove(order.getMessageId());
                        orderQueue.remove(order.getSequenceNumber());

                        System.out.println(chatMessage.getSender()+": "+chatMessage.getContent());

                        rg.incrementAndGet();

                    }
                }
            }
        }
    }

    public void bDeliver(Socket client) {

        try {

            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            Object obj = in.readObject();

            if(obj instanceof ChatMessage){

                ChatMessage chatMessage = (ChatMessage) obj;
                holdBackQueue.put(chatMessage.getId(), chatMessage);

            }else if( obj instanceof OrderMessage){

                OrderMessage orderMessage = (OrderMessage) obj;
                orderQueue.put(orderMessage.getSequenceNumber(),orderMessage);

            }

            client.close();

        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

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

    public void listenToKeyboard(){

        System.out.println("Chat Iniciado!");

        while (true) {

            System.out.print(processConfig.getId() + ": ");
            String contentMessage = scan.nextLine();

            if (contentMessage.equalsIgnoreCase("Close Chat")) {
                break;
            }

            ChatMessage chatMessage = new ChatMessage(processConfig.getId(), contentMessage);

            bMulticast(chatMessage);
        }

    }

    public void listenMessages(){

        while (true) {

            try {

                Socket client = server.accept();

                receivePool.submit(() -> bDeliver(client));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }


    @Override
    public void run() {

        new Thread(this::refreshMessages).start();

        new Thread(this::listenMessages).start();

        new Thread(this::listenToKeyboard).start();

    }

}
