package fr.rammex.chaseTag.game.arena.creation.event;

import fr.rammex.chaseTag.game.arena.creation.ArenaTool;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ArenaCreationEvent implements Listener {
    private final ArenaTool arenaTool = new ArenaTool();
    private static final Map<Player, Location> firstPositions = new HashMap<>();
    private static final Map<Player, Location> secondPositions = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(item != null && item.getType().equals(Material.STICK)){
            if(item.isSimilar(arenaTool.getArenaTool())){
                event.setCancelled(true);
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    player.sendMessage("§a1ère position définie : §f" + formatLoc(event.getClickedBlock().getLocation()));
                    firstPositions.put(player, event.getClickedBlock().getLocation());
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    player.sendMessage("§a2ème position définie : §f" + formatLoc(event.getClickedBlock().getLocation()));
                    secondPositions.put(player, event.getClickedBlock().getLocation());
                }
            }
        }
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    public static Location getFirstPosition(Player player){
        return firstPositions.get(player);
    }

    public static Location getSecondPosition(Player player){
        return secondPositions.get(player);
    }
}
