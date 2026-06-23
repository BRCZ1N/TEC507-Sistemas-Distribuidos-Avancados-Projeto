package com.chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

class ProcessSequencer extends Thread {

    private NodeConfig process;
    private AtomicLong clock;
    private ServerSocket server;
    private final Scanner scan = new Scanner(System.in);

    public ProcessSequencer(NodeConfig process) throws IOException {

        this.clock = new AtomicLong(0);
        this.server = new ServerSocket(process.getPort());
        this.process = process;
        start();

    }

    public AtomicLong getClock() {

        return this.clock;

    }

    public void setClock(AtomicLong clock) {

        this.clock = clock;

    }

    public void localEvent() {

        clock.incrementAndGet();
        System.out.println("Evento local - Relógio: " + this.clock.get());

    }

    public void receiveEvent(Socket client) throws IOException {

        try (
                ObjectInputStream in = new ObjectInputStream(client.getInputStream())
        ) {

            ChatMessage chatMessage = (ChatMessage) in.readObject();
            long previousClock = clock.get();
            clock.updateAndGet(current -> Math.max(current, chatMessage.getClock()) + 1);

            System.out.println("=========================================");
            System.out.println("Mensagem Recebida do Processo: " + chatMessage.getFromId());
            System.out.println("Relógio Local Antes: " + previousClock);
            System.out.println("Relógio na Mensagem: " + chatMessage.getClock());
            System.out.println("Relógio Local Atualizado: " + clock.get());
            System.out.println("Conteúdo: " + chatMessage.getContent());
            System.out.println("=========================================");

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        client.close();
    }

    public void sendEvent(NodeConfig destination){

        System.out.println("Digite a mensagem a ser enviada:");
        String contentMessage = scan.nextLine();

        try{

            Socket socket = new Socket(destination.getHost(), destination.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            clock.incrementAndGet();
            ChatMessage chatMessage = new ChatMessage(process.getId(),destination.getId(),clock.get(),contentMessage);

            System.out.println("=========================================");
            System.out.println("Mensagem Enviada");
            System.out.println("Processo Origem: " + chatMessage.getFromId());
            System.out.println("Processo Destino: " + chatMessage.getToId());
            System.out.println("Relógio Local Após Incremento: " + clock.get());
            System.out.println("Relógio Enviado: " + chatMessage.getClock());
            System.out.println("Conteúdo: " + chatMessage.getContent());
            System.out.println("=========================================");

            out.writeObject(chatMessage);
            out.flush();
            out.close();
            socket.close();

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("Não foi possível conectar ao processo destino.");
        }

    }

    public void menuMessage() {

        NodeConfig destination;

        while(true){

            System.out.println("Menu de mensagens - Lamport");
            System.out.println("1 - Evento Local");
            System.out.println("2 - Envio de mensagem");
            System.out.println("3 - Sair");
            String option = scan.nextLine();

            switch (option) {

                case "1":
                    localEvent();
                    break;

                case "2":

                    System.out.println("Escolha o processo que irá receber a mensagem:");
                    String optionId;

                    switch (process) {

                        case P1:

                            System.out.println("1 - P2");
                            System.out.println("2 - P3");
                            optionId = scan.nextLine();
                            switch (optionId) {

                                case "1":
                                    destination =  NodeConfig.valueOf("P2");
                                    sendEvent(destination);
                                    break;

                                case "2":
                                    destination = NodeConfig.valueOf("P3");
                                    sendEvent(destination);
                                    break;

                                default:
                                    System.out.println("P1 não pode enviar para si mesmo.");
                            }

                            break;

                        case P2:

                            System.out.println("1 - P1");
                            System.out.println("2 - P3");
                            optionId = scan.nextLine();

                            switch (optionId) {

                                case "1":
                                    destination = NodeConfig.valueOf("P1");
                                    sendEvent(destination);
                                    break;

                                case "2":
                                    destination = NodeConfig.valueOf("P3");
                                    sendEvent(destination);
                                    break;

                                default:
                                    System.out.println("P2 não pode enviar para si mesmo.");
                            }

                            break;

                        case P3:

                            System.out.println("1 - P1");
                            System.out.println("2 - P2");
                            optionId = scan.nextLine();

                            switch (optionId) {

                                case "1":
                                    destination = NodeConfig.valueOf("P1");
                                    sendEvent(destination);
                                    break;

                                case "2":
                                    destination = NodeConfig.valueOf("P2");
                                    sendEvent(destination);
                                    break;

                                default:
                                    System.out.println("P3 não pode enviar para si mesmo.");
                            }

                            break;
                    }

                    break;

                case "3":

                    return;
            }


        }

    }


    @Override
    public void run() {

        new Thread(() -> {
            menuMessage();
        }).start();

        while (true) {

            try {

                Socket client = server.accept();

                new Thread(() -> {
                    try {
                        receiveEvent(client);
                    } catch (IOException e) {
                        System.out.println("Erro ao receber mensagem.");
                    }
                }).start();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

    }

}
