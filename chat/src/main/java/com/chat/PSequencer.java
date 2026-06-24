package com.chat;

import java.io.IOException;

class PSequencer {

    public static void main(String[] args) {

        try {

            ProcessSequencer process = new ProcessSequencer(NodeConfig.valueOf(args[0]));

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}

