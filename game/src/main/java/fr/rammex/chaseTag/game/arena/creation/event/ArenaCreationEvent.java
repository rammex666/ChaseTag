package fr.rammex.chaseTag.arena.creation.event;

import fr.rammex.chaseTag.arena.creation.ArenaTool;
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
    private static Map<Player, Location> firstPositions = new HashMap<>();
    private static Map<Player, Location> secondPositions = new HashMap<>();

    @SuppressWarnings({ "deprecation", "incomplete-switch" })
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(item != null && item.getType().equals(Material.STICK)){
            if(item.equals(arenaTool.getArenaTool())){
                event.setCancelled(true);
                switch (event.getAction()) {
                    case Action.LEFT_CLICK_BLOCK:
                        player.sendMessage("1ère position définit");
                        firstPositions.put(player, event.getClickedBlock().getLocation());
                        break;
                    case Action.RIGHT_CLICK_BLOCK:
                        player.sendMessage("2ème position définit");
                        secondPositions.put(player, event.getClickedBlock().getLocation());
                        break;
                }
            }
        }

    }

    public static Location getFirstPosition(Player player){
        return firstPositions.get(player);
    }

    public static Location getSecondPosition(Player player){
        return secondPositions.get(player);
    }
}
