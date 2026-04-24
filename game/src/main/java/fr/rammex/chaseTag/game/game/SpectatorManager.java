package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SpectatorManager {

    public static void giveSpectatorItems(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Spectateur (Clic-droit)");
            compass.setItemMeta(meta);
        }
        
        player.getInventory().setItem(0, compass);
    }

    public static void handleSpectatorInteract(org.bukkit.entity.Player player, Game game) {
        if (game == null) return;
        
        List<Player> players = game.getPlayers();
        if (players.isEmpty()) return;

        // Simple cycling through players for now
        // We could open an inventory but for 1v1 it's faster to just cycle or teleport to a random one
        // Let's teleport to the first player who is online
        for (Player p : players) {
            org.bukkit.entity.Player bp = p.getBukkitPlayer();
            if (bp != null && !bp.equals(player)) {
                player.teleport(bp.getLocation());
                player.sendMessage(ChatColor.GRAY + "Téléportation vers " + ChatColor.YELLOW + bp.getName());
                break;
            }
        }
    }
}
