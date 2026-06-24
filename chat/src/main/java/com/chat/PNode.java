package com.chat;

import java.io.IOException;

class PNode {

    public static void main(String[] args) {

        try {

            ProcessNode process = new ProcessNode(NodeConfig.valueOf(args[0]));

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}

