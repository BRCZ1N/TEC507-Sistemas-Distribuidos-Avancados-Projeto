package chat.sda.spring.model;

public class Node {

    private final Long id;
    private final String host;
    private final Integer port;

    public Node(Long id, String host, Integer port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }


}
