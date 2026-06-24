package com.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

class ProcessSequencer extends Thread {

    private NodeConfig processConfig;
    private ServerSocket server;
    private AtomicLong sg;
    private ExecutorService sendPool = Executors.newFixedThreadPool(3);
    private ExecutorService receivePool = Executors.newFixedThreadPool(3);

    public ProcessSequencer(NodeConfig process) throws IOException {

        this.sg = new AtomicLong(0);
        this.server = new ServerSocket(process.getPort());
        this.processConfig = process;
        start();

    }

    public void bDeliver(Socket client) {

        try {

            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ChatMessage chat = (ChatMessage) in.readObject();
            long currentSg = sg.incrementAndGet();
            OrderMessage order = new OrderMessage(chat.getId(),currentSg);
            bMulticast(order);
            client.close();

        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void bMulticast(OrderMessage message){

        for(NodeConfig process: NodeConfig.values()) {

            sendPool.submit(() -> {

                if(!processConfig.equals(process)){

                    try(Socket socket = new Socket(process.getHost(), process.getPort())){

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(message);
                        out.flush();

                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        System.out.println("Falha ao enviar para: " + processConfig.getHost());
                    }

                }

            });

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

        listenMessages();

    }

}
