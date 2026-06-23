package com.chat;

import java.io.IOException;

class Main {

    public static void main(String[] args) {

        try {

            ProcessNode lamport = new ProcessNode(NodeConfig.valueOf(args[0]));

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}

