package fr.rammex.chaseTag.game.player;

import java.util.HashMap;
import java.util.Map;

public class PlayerProfile {
    private final String uuid;
    private Map<String, Object> playerData;

    public PlayerProfile(String uuid) {
        this.uuid = uuid;
        this.playerData = new HashMap<>();
    }

    public String getUuid() {
        return uuid;
    }

    public Map<String, Object> getPlayerData() {
        return playerData;
    }

    public void setPlayerData(Map<String, Object> playerData) {
        this.playerData = playerData;
    }
}
