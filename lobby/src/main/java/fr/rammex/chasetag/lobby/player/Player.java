package fr.rammex.chasetag.lobby.player;

import fr.rammex.chasetag.lobby.player.rank.Rank;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;

public class Player {
    private final String playerUUID;
    private String playerName;
    private Rank playerRank;
    private Map<String, Object> playerData;

    public Player(String playerUUID, String playerName, Rank playerRank) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.playerRank = playerRank;
        this.playerData = new java.util.HashMap<>();
    }

    public Rank getPlayerRole() {
        return playerRank;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerRole(Rank playerRank) {
        this.playerRank = playerRank;
    }

    public Map<String, Object> getPlayerData() {
        return playerData;
    }

    public Object getPlayerData(String key) {
        return playerData.get(key);
    }

    public void setPlayerData(String key, Object value) {
        this.playerData.put(key, value);
    }

    public void setPlayerData(Map<String, Object> playerData) {
        this.playerData = playerData;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return Bukkit.getPlayer(UUID.fromString(playerUUID));
    }
}
