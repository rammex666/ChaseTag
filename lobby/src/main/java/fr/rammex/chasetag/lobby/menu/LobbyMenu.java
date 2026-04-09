package fr.rammex.chasetag.lobby.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class LobbyMenu extends Menu {
    private Inventory inventory;

    public LobbyMenu(Player player) {
        super(player);
        this.inventory = Bukkit.createInventory(this, 54, "Menu Principal");
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        // Add a button at the bottom right (slot 53) for admins
        if (player.hasPermission("chasetag.admin.menu")) {
            ItemStack adminButton = new ItemStack(Material.REDSTONE_TORCH);
            ItemMeta meta = adminButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Menu Administrateur");
                adminButton.setItemMeta(meta);
            }
            inventory.setItem(53, adminButton);
        }

        // Fill background with glass panes for a nice look
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
