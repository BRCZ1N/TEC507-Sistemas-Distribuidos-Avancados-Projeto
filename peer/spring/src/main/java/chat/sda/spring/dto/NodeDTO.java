package chat.sda.spring.dto;

import jakarta.validation.constraints.NotNull;

public class NodeDTO {

    @NotNull
    private String id;
    @NotNull
    private String host;
    @NotNull
    private int port;

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
