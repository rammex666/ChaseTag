package fr.rammex.chasetag.common.message;

import java.util.List;

public class GameEndMessage {

    private String serverId;
    private String winnerUuid;
    private List<String> playerUuids;

    public GameEndMessage() {}

    public GameEndMessage(String serverId, String winnerUuid, List<String> playerUuids) {
        this.serverId = serverId;
        this.winnerUuid = winnerUuid;
        this.playerUuids = playerUuids;
    }

    public String getServerId() { return serverId; }
    public String getWinnerUuid() { return winnerUuid; }
    public List<String> getPlayerUuids() { return playerUuids; }
}