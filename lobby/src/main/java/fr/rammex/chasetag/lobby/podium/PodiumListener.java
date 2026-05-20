package fr.rammex.chasetag.lobby.podium;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PodiumListener implements Listener {
    private final ChaseTagLobby plugin;

    public PodiumListener(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata("podium_interaction")) {
            event.setCancelled(true);
            plugin.getPodiumManager().cycleStat();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().hasMetadata("podium_interaction")) {
            event.setCancelled(true);
            if (event.getDamager() instanceof org.bukkit.entity.Player) {
                plugin.getPodiumManager().cycleStat();
            }
        }
    }
}
