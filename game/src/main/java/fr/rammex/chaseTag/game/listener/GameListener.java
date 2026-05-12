package fr.rammex.chaseTag.game.listener;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.player.PlayerProfile;
import fr.rammex.chaseTag.game.staff.StaffManager;
import fr.rammex.chasetag.common.RedisChannel;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;

public class GameListener implements Listener {

    private final ChaseTag plugin;

    public GameListener(ChaseTag plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getPlayerMongoRepository().getPlayerByUUID(p.getUniqueId().toString()).ifPresent(profile -> {
                if (Boolean.TRUE.equals(profile.getPlayerData().get("staff_mode"))) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getStaffManager().applyStaffMode(p);
                    });
                }
            });
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getItemMeta() == null || !plugin.getStaffManager().isStaffInGame(player.getUniqueId())) {
            return;
        }

        if (item.getType() == Material.NETHER_STAR && item.getItemMeta().getDisplayName().contains("Retour au Lobby")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                sendPlayerToLobby(player);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Player target) {
            Player staff = event.getPlayer();
            if (!plugin.getStaffManager().isStaffInGame(staff.getUniqueId())) return;

            ItemStack item = staff.getInventory().getItemInMainHand();
            if (item.getType() == Material.ICE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Geler")) {
                staff.sendMessage(ChatColor.RED + "La fonction de freeze n'est pas implémentée sur les serveurs de jeu pour le moment.");
                target.sendMessage(ChatColor.AQUA + staff.getName() + " a essayé de vous geler.");
                event.setCancelled(true);
            }
        }
    }

    private void sendPlayerToLobby(Player player) {
        String lobbyServerName = plugin.getConfig().getString("lobby-server-name", "lobby");
        player.sendMessage(ChatColor.GRAY + "Téléportation vers le lobby...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getJedisPool().getResource()) {
                jedis.publish(RedisChannel.SEND_TO_LOBBY, player.getUniqueId().toString() + ":" + lobbyServerName);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not publish to Redis to send player to lobby: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "Erreur lors de la connexion au lobby.");
            }
        });
    }
}
