package fr.rammex.chasetag.lobby.listener;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.menu.LobbyMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import fr.rammex.chasetag.lobby.menu.StaffTeleportMenu;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyListener implements Listener {

    private final ChaseTagLobby plugin;

    public LobbyListener(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveLobbyItems(player);
        player.setFoodLevel(20);
        player.setHealth(20);

        // Cacher le staff déjà présent pour le nouveau joueur
        plugin.getStaffModeManager().hideStaffFrom(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().hasPermission("chasetag.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("chasetag.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    private void giveLobbyItems(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Menu Principal " + ChatColor.GRAY + "(Clic Droit)");
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(4, compass);

        updateReadyItem(player);
    }

    public void updateReadyItem(Player player) {
        var tournamentManager = plugin.getTournamentManager();
        var matchOpt = tournamentManager.getMatchForPlayer(player.getName());

        if (matchOpt.isEmpty()) {
            player.getInventory().setItem(8, null);
            return;
        }

        var match = matchOpt.get();
        boolean isReady = match.isPlayerReady(player.getName());

        ItemStack item = new ItemStack(isReady ? Material.GREEN_CONCRETE : Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(isReady ? ChatColor.GREEN + "Prêt" : ChatColor.RED + "Pas Prêt");
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(8, item);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isCompass(item) || isReadyItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getStaffModeManager().isFrozen(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Player target) {
            Player staff = event.getPlayer();
            ItemStack item = staff.getInventory().getItemInMainHand();
            if (item.getType() == Material.ICE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Geler")) {
                plugin.getStaffModeManager().toggleFreeze(staff, target);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (isCompass(item)) {
                event.setCancelled(true);
                if (event.getAction().name().contains("RIGHT")) {
                    if (plugin.getStaffModeManager().isStaffMode(player.getUniqueId())) {
                        new StaffTeleportMenu(player).open();
                    } else {
                        new LobbyMenu(player).open();
                    }
                }
            } else if (isReadyItem(item)) {
                toggleReady(player);
                event.setCancelled(true);
            }
        }
    }

    private void toggleReady(Player player) {
        var tournamentManager = plugin.getTournamentManager();
        var matchOpt = tournamentManager.getMatchForPlayer(player.getName());

        if (matchOpt.isPresent()) {
            var match = matchOpt.get();
            boolean currentReady = match.isPlayerReady(player.getName());
            tournamentManager.setPlayerReady(match.getPhase(), match.getPool(), match.getMatchId(), player.getName(), !currentReady);
            updateReadyItem(player);
            player.sendMessage(ChatColor.YELLOW + "Statut prêt : " + (!currentReady ? ChatColor.GREEN + "PRÊT" : ChatColor.RED + "PAS PRÊT"));
            
            // Notify opponent
            String opponentName = player.getName().equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
            if (opponentName != null) {
                Player opponent = Bukkit.getPlayerExact(opponentName);
                if (opponent != null) {
                    opponent.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " est maintenant " + (!currentReady ? ChatColor.GREEN + "PRÊT" : ChatColor.RED + "PAS PRÊT"));
                    updateReadyItem(opponent);
                }
            }
        }
    }

    private boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String name = meta.getDisplayName();
        return name.contains("Menu Principal") || name.contains("Téléportation aux parties");
    }

    private boolean isReadyItem(ItemStack item) {
        if (item == null || (item.getType() != Material.RED_CONCRETE && item.getType() != Material.GREEN_CONCRETE)) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && (meta.getDisplayName().contains("Prêt") || meta.getDisplayName().contains("Pas Prêt"));
    }
}
