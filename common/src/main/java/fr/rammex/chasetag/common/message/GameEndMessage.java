package fr.rammex.chasetag.common.message;

import java.util.List;
import java.util.Map;

public class GameEndMessage {

    private String serverId;
    private String winnerUuid;
    private String winnerName;
    private List<String> playerUuids;
    private Map<String, Integer> playerScores;

    public GameEndMessage() {}

    public GameEndMessage(String serverId, String winnerUuid, String winnerName, List<String> playerUuids, Map<String, Integer> playerScores) {
        this.serverId = serverId;
        this.winnerUuid = winnerUuid;
        this.winnerName = winnerName;
        this.playerUuids = playerUuids;
        this.playerScores = playerScores;
    }

    public String getServerId() { return serverId; }
    public String getWinnerUuid() { return winnerUuid; }
    public String getWinnerName() { return winnerName; }
    public List<String> getPlayerUuids() { return playerUuids; }
    public Map<String, Integer> getPlayerScores() { return playerScores; }
}