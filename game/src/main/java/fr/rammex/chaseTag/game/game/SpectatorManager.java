package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class SpectatorManager {

    public static void giveSpectatorItems(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Sélecteur de Joueurs " + ChatColor.GRAY + "(Clic-droit)");
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(0, compass);

        ItemStack quit = new ItemStack(Material.RED_BED);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName(ChatColor.RED + "Quitter la partie " + ChatColor.GRAY + "(Clic-droit)");
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    public static void handleSpectatorInteract(org.bukkit.entity.Player player, Game game) {
        if (game == null) return;
        
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Sélecteur de Joueurs");
        
        for (Player p : game.getPlayers()) {
            org.bukkit.entity.Player bp = p.getBukkitPlayer();
            if (bp != null) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(bp);
                    skullMeta.setDisplayName(ChatColor.YELLOW + bp.getName());
                    head.setItemMeta(skullMeta);
                }
                inv.addItem(head);
            }
        }
        
        player.openInventory(inv);
    }

    public static void handleMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Sélecteur de Joueurs")) {
            event.setCancelled(true);
            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType() != Material.PLAYER_HEAD) return;
            
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
            SkullMeta skullMeta = (SkullMeta) current.getItemMeta();
            
            if (skullMeta != null && skullMeta.getOwningPlayer() != null) {
                org.bukkit.entity.Player target = Bukkit.getPlayer(skullMeta.getOwningPlayer().getUniqueId());
                if (target != null) {
                    player.teleport(target.getLocation());
                    player.sendMessage(ChatColor.GRAY + "Téléportation vers " + ChatColor.YELLOW + target.getName());
                    player.closeInventory();
                }
            }
        }
    }
}
