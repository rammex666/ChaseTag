package fr.rammex.chasetag.lobby.staff;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class StaffManager {

    private final ChaseTagLobby plugin;
    private final Map<String, Sanction> pendingSanctions = new HashMap<>();

    public StaffManager(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    public String createPendingSanction(Sanction.Type type, String targetName, UUID targetUuid, String reason, String staffName) {
        String id = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        Sanction sanction = new Sanction(id, type, targetName, targetUuid, reason, staffName);
        pendingSanctions.put(id, sanction);

        // Envoyer sur Discord
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendStaffSanctionRequest(sanction);
        }

        return id;
    }

    public boolean acceptSanction(String id) {
        Sanction sanction = pendingSanctions.remove(id);
        if (sanction == null) return false;

        applySanction(sanction);
        return true;
    }

    private void applySanction(Sanction sanction) {
        plugin.getPlayerMongoRepository().getPlayerByUUID(sanction.getTargetUuid().toString()).ifPresent(player -> {
            if (sanction.getType() == Sanction.Type.BAN) {
                player.setPlayerData("banned", true);
                player.setPlayerData("ban_reason", sanction.getReason());
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(sanction.getTargetUuid());
                    if (onlinePlayer != null) {
                        onlinePlayer.kickPlayer(ChatColor.RED + "Vous avez été banni pour : " + ChatColor.WHITE + sanction.getReason());
                    }
                });
            } else if (sanction.getType() == Sanction.Type.MUTE) {
                player.setPlayerData("muted", true);
                player.setPlayerData("mute_reason", sanction.getReason());

                org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(sanction.getTargetUuid());
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(ChatColor.RED + "Vous avez été rendu muet pour : " + ChatColor.WHITE + sanction.getReason());
                }
            }
            plugin.getPlayerMongoRepository().savePlayer(player);
        });
    }

    public void unmute(UUID targetUuid) {
        plugin.getPlayerMongoRepository().getPlayerByUUID(targetUuid.toString()).ifPresent(player -> {
            player.setPlayerData("muted", false);
            player.setPlayerData("mute_reason", null);
            plugin.getPlayerMongoRepository().savePlayer(player);
            
            org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(targetUuid);
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(ChatColor.GREEN + "Vous avez été rendu la parole par un administrateur.");
            }
        });
    }

    public void unban(UUID targetUuid) {
        plugin.getPlayerMongoRepository().getPlayerByUUID(targetUuid.toString()).ifPresent(player -> {
            player.setPlayerData("banned", false);
            player.setPlayerData("ban_reason", null);
            plugin.getPlayerMongoRepository().savePlayer(player);
        });
    }

    public Map<String, Sanction> getPendingSanctions() {
        return pendingSanctions;
    }
}
