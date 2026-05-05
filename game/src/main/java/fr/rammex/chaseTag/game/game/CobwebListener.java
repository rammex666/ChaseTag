package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CobwebListener implements Listener {

    private final ChaseTag plugin;

    public CobwebListener(ChaseTag plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COBWEB) return;
        
        Game game = plugin.getGameManager().getGame();
        if (game == null || game.getGameState() != GameState.PLAYING || game.isCountdown()) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Block targetCenter = clickedBlock.getRelative(event.getBlockFace());
        Arena arena = game.getArena();

        List<Block> cobwebs = new ArrayList<>();
        // Créer une box 2x2 (sur le plan horizontal XZ, ou vertical selon le besoin, ici on va faire X et Z)
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                Block b = targetCenter.getRelative(x, 0, z);
                if (arena.isInside(b.getLocation()) && (b.getType() == Material.AIR || b.getType() == Material.RED_WOOL || b.getType() == Material.BLUE_WOOL)) {
                    cobwebs.add(b);
                }
            }
        }

        if (cobwebs.isEmpty()) return;

        // Retirer l'item de la main (on en a qu'un par manche selon la demande, ou on gère le stack)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Poser les toiles
        for (Block b : cobwebs) {
            b.setType(Material.COBWEB);
        }

        // Les retirer après 2 secondes (40 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Block b : cobwebs) {
                if (b.getType() == Material.COBWEB) {
                    b.setType(Material.AIR);
                }
            }
        }, 40L);
    }
}
