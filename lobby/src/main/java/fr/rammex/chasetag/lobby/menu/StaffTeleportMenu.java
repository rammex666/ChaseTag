package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.game.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffTeleportMenu extends Menu {

    private final ChaseTagLobby plugin;

    public StaffTeleportMenu(Player player) {
        super(player);
        this.plugin = ChaseTagLobby.getInstance();
        this.inventory = Bukkit.createInventory(this, 54, "Parties en cours");
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    public void setMenuItems() {
        inventory.clear();
        int slot = 0;
        for (GameSession session : plugin.getGameManager().getSessions().values()) {
            if (session.getStatus() == GameSession.Status.PLAYING) {
                ItemStack item = createItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Partie: " + ChatColor.WHITE + session.getSessionId(),
                        ChatColor.GRAY + "Serveur: " + ChatColor.YELLOW + session.getPterodactylServerId(),
                        ChatColor.GRAY + "Map: " + ChatColor.YELLOW + session.getMapName(),
                        ChatColor.GRAY + "Joueurs:",
                        getPlayersNames(session),
                        ChatColor.AQUA + "Clic pour rejoindre (Spectateur)");
                
                inventory.setItem(slot++, item);
            }
        }
    }

    private String getPlayersNames(GameSession session) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : session.getPlayers()) {
            sb.append(ChatColor.WHITE).append("- ").append(Bukkit.getOfflinePlayer(uuid).getName()).append(" ");
        }
        return sb.toString();
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> list = new ArrayList<>();
            for (String s : lore) {
                list.add(s);
            }
            meta.setLore(list);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) return;

        String sessionName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (sessionName.startsWith("Partie: ")) {
            String sessionId = sessionName.replace("Partie: ", "");
            GameSession session = plugin.getGameManager().getSessions().get(sessionId);
            
            if (session != null) {
                String serverId = session.getPterodactylServerId();
                player.sendMessage(ChatColor.GREEN + "Téléportation vers " + serverId + "...");
                
                try (Jedis jedis = plugin.getJedisPool().getResource()) {
                    jedis.publish(RedisChannel.SEND_TO_LOBBY,
                        player.getUniqueId().toString() + ":" + serverId);
                }
                player.closeInventory();
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
