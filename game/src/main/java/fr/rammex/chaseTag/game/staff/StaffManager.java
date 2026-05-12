package fr.rammex.chaseTag.game.staff;

import fr.rammex.chaseTag.game.ChaseTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaffManager {

    private final ChaseTag plugin;
    private final Set<UUID> staffInGame = new HashSet<>();

    public StaffManager(ChaseTag plugin) {
        this.plugin = plugin;
    }

    public void applyStaffMode(Player player) {
        staffInGame.add(player.getUniqueId());
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("chasetag.staff")) {
                online.hidePlayer(plugin, player);
            }
        }

        fr.rammex.chaseTag.game.game.Game currentGame = plugin.getGameManager().getGame();
        if (currentGame != null) {
            fr.rammex.chaseTag.game.player.Player gamePlayer = fr.rammex.chaseTag.game.player.PlayerManager.getPlayer(player.getUniqueId().toString());
            if (gamePlayer != null && !currentGame.getSpectators().contains(gamePlayer)) {
                currentGame.getSpectators().add(gamePlayer);
            }
        }

        giveStaffItems(player);
        player.sendMessage(ChatColor.AQUA + "Mode Staff actif sur ce serveur de jeu.");
    }

    public boolean isStaffInGame(UUID uuid) {
        return staffInGame.contains(uuid);
    }

    private void giveStaffItems(Player player) {
        player.getInventory().clear();

        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = star.getItemMeta();
        starMeta.setDisplayName(ChatColor.AQUA + "Retour au Lobby " + ChatColor.GRAY + "(Clic Droit)");
        star.setItemMeta(starMeta);

        ItemStack ice = new ItemStack(Material.ICE);
        ItemMeta iceMeta = ice.getItemMeta();
        iceMeta.setDisplayName(ChatColor.BLUE + "Geler un joueur " + ChatColor.GRAY + "(Clic Droit sur le joueur)");
        ice.setItemMeta(iceMeta);

        player.getInventory().setItem(0, star);
        player.getInventory().setItem(1, ice);
    }
}