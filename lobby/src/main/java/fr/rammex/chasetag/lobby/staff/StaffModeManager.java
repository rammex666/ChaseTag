package fr.rammex.chasetag.lobby.staff;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffModeManager {

    private final ChaseTagLobby plugin;
    private final Set<UUID> staffModePlayers = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, ItemStack[]> inventoryBackups = new HashMap<>();

    public StaffModeManager(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    public void toggleStaffMode(Player player) {
        if (staffModePlayers.contains(player.getUniqueId())) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    private void enableStaffMode(Player player) {
        staffModePlayers.add(player.getUniqueId());
        inventoryBackups.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
        
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        
        // Cacher le staff des autres joueurs
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId()) && !online.hasPermission("chasetag.staff")) {
                online.hidePlayer(plugin, player);
            }
        }

        giveStaffItems(player);
        updateStaffModeInDb(player, true);
        player.sendMessage(ChatColor.GREEN + "Mode Staff activé !");
    }

    private void disableStaffMode(Player player) {
        staffModePlayers.remove(player.getUniqueId());
        player.getInventory().clear();
        
        if (inventoryBackups.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(inventoryBackups.remove(player.getUniqueId()));
        }
        
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);

        // Réafficher le staff pour tout le monde
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
        
        updateStaffModeInDb(player, false);
        player.sendMessage(ChatColor.RED + "Mode Staff désactivé !");
    }

    private void updateStaffModeInDb(Player player, boolean enabled) {
        fr.rammex.chasetag.lobby.player.Player ctPlayer = fr.rammex.chasetag.lobby.player.PlayerManager.getPlayer(player.getUniqueId().toString());
        if (ctPlayer != null) {
            ctPlayer.setPlayerData("staff_mode", enabled);
            plugin.getPlayerMongoRepository().savePlayer(ctPlayer);
        }
    }

    public void hideStaffFrom(Player player) {
        for (UUID staffUuid : staffModePlayers) {
            Player staff = Bukkit.getPlayer(staffUuid);
            if (staff != null) {
                player.hidePlayer(plugin, staff);
            }
        }
    }

    private void giveStaffItems(Player player) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = star.getItemMeta();
        if (starMeta != null) {
            starMeta.setDisplayName(ChatColor.AQUA + "Téléportation aux parties " + ChatColor.GRAY + "(Clic Droit)");
            star.setItemMeta(starMeta);
        }
        
        ItemStack ice = new ItemStack(Material.ICE);
        ItemMeta iceMeta = ice.getItemMeta();
        if (iceMeta != null) {
            iceMeta.setDisplayName(ChatColor.BLUE + "Geler un joueur " + ChatColor.GRAY + "(Clic Droit sur le joueur)");
            ice.setItemMeta(iceMeta);
        }

        player.getInventory().setItem(0, star);
        player.getInventory().setItem(1, ice);
    }

    public void toggleFreeze(Player staff, Player target) {
        if (frozenPlayers.contains(target.getUniqueId())) {
            frozenPlayers.remove(target.getUniqueId());
            target.sendMessage(ChatColor.GREEN + "Vous avez été dégelé.");
            staff.sendMessage(ChatColor.GREEN + "Vous avez dégelé " + ChatColor.AQUA + target.getName() + ".");
        } else {
            frozenPlayers.add(target.getUniqueId());
            target.sendMessage(ChatColor.RED + "Vous avez été GELER par un staff ! Ne déconnectez pas !");
            staff.sendMessage(ChatColor.RED + "Vous avez gelé " + ChatColor.AQUA + target.getName() + ".");
        }
    }

    public boolean isStaffMode(UUID uuid) {
        return staffModePlayers.contains(uuid);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public Set<UUID> getFrozenPlayers() {
        return frozenPlayers;
    }
}
