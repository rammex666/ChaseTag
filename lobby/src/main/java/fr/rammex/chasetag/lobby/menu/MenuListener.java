package fr.rammex.chasetag.lobby.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Menu) {
            event.setCancelled(true); // Prevent moving items in the menu

            if (event.getCurrentItem() == null) return;

            // Handle specific button clicks if needed
            // For now, we just know slot 53 is the admin button in LobbyMenu
            if (holder instanceof LobbyMenu) {
                if (event.getSlot() == 53) {
                    event.getWhoClicked().sendMessage("§cLe menu administrateur sera disponible bientôt !");
                    // We can open the next menu here later
                }
            }
        } else {
            // Check if player is trying to move the compass in their own inventory
            if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Menu Principal")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
