package fr.rammex.chaseTag.game.player;

import org.bukkit.Bukkit;
import java.util.UUID;

public class Player {
    private final String playerUUID;
    private Role playerRole;

    public Player(String playerUUID,Role playerRole){
        this.playerUUID = playerUUID;
        this.playerRole = playerRole;
    }

    public Role getPlayerRole() {
        return playerRole;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerRole(Role playerRole) {
        this.playerRole = playerRole;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return Bukkit.getPlayer(UUID.fromString(playerUUID));
    }
}
