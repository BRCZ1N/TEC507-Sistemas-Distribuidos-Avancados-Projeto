package com.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

class ProcessNode extends Thread {

    private NodeConfig processConfig;
    private ServerSocket server;
    private Map<String,ChatMessage> holdBackQueue = new ConcurrentHashMap<>();
    private Map<Long,OrderMessage> orderQueue = new ConcurrentHashMap<>();
    private final Scanner scan = new Scanner(System.in);

    public ProcessNode(NodeConfig process) throws IOException {

        this.server = new ServerSocket(process.getPort());
        this.processConfig = process;
        start();

    }

    public void bDeliver(Socket client) {

        try {

            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ChatMessage chatMessage = (ChatMessage) in.readObject();
            client.close();

        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void bMulticast(ChatMessage message){

        try{

            for(NodeConfig process: NodeConfig.values()){

                Socket socket = new Socket(process.getHost(), process.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                out.writeObject(message);
                out.flush();
                out.close();
                socket.close();

            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("Não foi possível conectar ao processo destino.");
        }

    }

    public void listenToKeyboard(){

        System.out.println("Chat Iniciado! Digite suas mensagens abaixo:");

        while (true) {

            System.out.print(processConfig.getId() + ": ");
            String contentMessage = scan.nextLine();

            if (contentMessage.equalsIgnoreCase("sair")) {
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

                new Thread(() -> {

                    bDeliver(client);

                }).start();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

    }


    @Override
    public void run() {

        new Thread(() -> {
            listenToKeyboard();
        }).start();

        new Thread(() -> {
            listenMessages();
        }).start();

    }

}
