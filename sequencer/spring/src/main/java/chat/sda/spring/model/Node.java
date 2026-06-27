package chat.sda.spring.model;

public class Node {

    private final String id;
    private final String host;
    private final int port;

    public Node(String id, String host, int port) {
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
