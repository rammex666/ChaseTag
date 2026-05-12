package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.menu.BracketMenu;
import fr.rammex.chasetag.lobby.menu.MapSelectionMenu;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {
    public static final java.util.Map<java.util.UUID, Integer> selectedMatch = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, String> selectedMatchPhase = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, Integer> selectedPool = new java.util.HashMap<>();

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.getItemMeta() == null) {
            return;
        }

        if (holder instanceof Menu menu) {
            event.setCancelled(true);
            menu.handleClick(event);
            return;
        }
 else {
            if (currentItem.getItemMeta().getDisplayName().contains("Menu Principal")) {
                event.setCancelled(true);
            }
        }
    }
}
