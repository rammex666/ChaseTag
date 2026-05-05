package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LobbyMenu extends Menu {

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
        // Stats button (Book)
        ItemStack statsButton = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsButton.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(ChatColor.YELLOW + "Vos Statistiques");
            statsMeta.setLore(List.of(ChatColor.GRAY + "Visualisez vos victoires et défaites."));
            statsButton.setItemMeta(statsMeta);
        }
        inventory.setItem(19, statsButton);

        // Duel button (Blaze Rod)
        ItemStack duelButton = new ItemStack(Material.BLAZE_ROD);
        ItemMeta duelMeta = duelButton.getItemMeta();
        if (duelMeta != null) {
            duelMeta.setDisplayName(ChatColor.GOLD + "Lancer un Duel");
            duelMeta.setLore(List.of(ChatColor.GRAY + "Affrontez un autre joueur ou entraînez-vous seul."));
            duelButton.setItemMeta(duelMeta);
        }
        inventory.setItem(21, duelButton);

        // Spectate button (Ender Eye)
        ItemStack spectateButton = new ItemStack(Material.ENDER_EYE);
        ItemMeta spectateMeta = spectateButton.getItemMeta();
        if (spectateMeta != null) {
            spectateMeta.setDisplayName(ChatColor.GREEN + "Regarder une partie");
            spectateMeta.setLore(List.of(ChatColor.GRAY + "Devenez spectateur d'un match en cours."));
            spectateButton.setItemMeta(spectateMeta);
        }
        inventory.setItem(23, spectateButton);

        // Map list / Info button (Map)
        ItemStack infoButton = new ItemStack(Material.MAP);
        ItemMeta infoMeta = infoButton.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.AQUA + "Informations");
            infoMeta.setLore(List.of(ChatColor.GRAY + "En savoir plus sur ChaseTag."));
            infoButton.setItemMeta(infoMeta);
        }
        inventory.setItem(25, infoButton);

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

        // Fill background with glass panes
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
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.GOLD + "Lancer un Duel")) {
            new PlayerSelectionMenu(player, ChaseTagLobby.getInstance()).open();
        } else if (displayName.equals(ChatColor.GREEN + "Regarder une partie")) {
            new ActiveGamesMenu(player).open();
        } else if (displayName.equals(ChatColor.RED + "Menu Administrateur")) {
            if (player.hasPermission("chasetag.admin.menu")) {
                new TournamentAdminMenu(player, ChaseTagLobby.getInstance().getTournamentManager()).open();
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

