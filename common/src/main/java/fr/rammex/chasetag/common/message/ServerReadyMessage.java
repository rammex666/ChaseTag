package fr.rammex.chasetag.common.message;

public class ServerReadyMessage {

    private String serverId;
    private int port;
    private String host;

    public ServerReadyMessage() {}

    public ServerReadyMessage(String serverId, int port, String host) {
        this.serverId = serverId;
        this.port = port;
        this.host = host;
    }

    public String getServerId() { return serverId; }
    public int getPort() { return port; }
    public String getHost() { return host; }
}