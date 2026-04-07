package fr.rammex.chasetag.common.message;

import java.util.List;

public class GameEndMessage {

    private String serverId;
    private String winnerUuid;
    private String winnerName;
    private List<String> playerUuids;

    public GameEndMessage() {}

    public GameEndMessage(String serverId, String winnerUuid, String winnerName, List<String> playerUuids) {
        this.serverId = serverId;
        this.winnerUuid = winnerUuid;
        this.winnerName = winnerName;
        this.playerUuids = playerUuids;
    }

    public String getServerId() { return serverId; }
    public String getWinnerUuid() { return winnerUuid; }
    public String getWinnerName() { return winnerName; }
    public List<String> getPlayerUuids() { return playerUuids; }
}