package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerSelectionMenu extends Menu {
    private final ChaseTagLobby plugin;

    public PlayerSelectionMenu(Player player, ChaseTagLobby plugin) {
        super(player);
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, "Sélectionner un joueur");
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        inventory.clear();

        // Background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        int slot = 10;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue;
            if (slot >= 44) break; // Limit to one page for now

            if (slot % 9 == 8) slot += 2;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(onlinePlayer);
                skullMeta.setDisplayName(ChatColor.YELLOW + onlinePlayer.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Cliquez pour défier en duel !");
                skullMeta.setLore(lore);
                skull.setItemMeta(skullMeta);
            }
            inventory.setItem(slot, skull);
            slot++;
        }

        ItemStack back = new ItemStack(Material.ARROW);
        var backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.YELLOW + "Retour");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(45, back);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new LobbyMenu(player).open();
            return;
        }

        if (item.getType() == Material.PLAYER_HEAD) {
            String targetName = ChatColor.stripColor(displayName);
            new MapSelectionMenu(player, plugin, targetName).open();
        }
    }
}
