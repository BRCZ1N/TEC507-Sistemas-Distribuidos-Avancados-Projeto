package com.chat;

public enum NodeConfig {

    P1("P1", "192.168.0.12", 5060),
    P2("P2", "192.168.0.12", 5061),
    P3("P3", "192.168.0.12", 5062),
    P4("P4", "192.168.0.12", 5063);

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
