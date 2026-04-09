package fr.rammex.chasetag.lobby.player;

import fr.rammex.chasetag.lobby.player.rank.Rank;
import org.bukkit.Bukkit;

import java.util.UUID;

public class Player {
    private final String playerUUID;
    private Rank playerRank;

    public Player(String playerUUID,Rank playerRank){
        this.playerUUID = playerUUID;
        this.playerRank = playerRank;
    }

    public Rank getPlayerRole() {
        return playerRank;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerRole(Rank playerRank) {
        this.playerRank = playerRank;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return Bukkit.getPlayer(UUID.fromString(playerUUID));
    }
}
