package com.chat;

public enum NodeConfig {

    P1("P1", "172.16.103.7", 5050),
    P2("P2", "172.16.103.7", 5051),
    P3("P3", "172.16.103.8", 5055),
    P4("P4", "172.16.103.8", 5055);

    private final String id;
    private final String host;
    private final int port;

    NodeConfig(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
