package fr.rammex.chasetag.lobby.menu;

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

import java.util.ArrayList;
import java.util.List;

public class ActiveGamesMenu extends Menu {
    private Inventory inventory;

    public ActiveGamesMenu(Player player) {
        super(player);
        this.inventory = Bukkit.createInventory(this, 54, "Parties en cours");
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        inventory.clear();
        
        ChaseTagLobby.getInstance().getGameManager().getSessions().values().forEach(session -> {
            if (session.getStatus() == GameSession.Status.PLAYING && session.getPterodactylServerId() != null) {
                ItemStack item = new ItemStack(Material.ENDER_EYE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + "Partie de " + Bukkit.getOfflinePlayer(session.getOwnerUuid()).getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + session.getType().name());
                    lore.add(ChatColor.GRAY + "Joueurs:");
                    session.getPlayers().forEach(uuid -> {
                        lore.add(ChatColor.GRAY + " - " + ChatColor.WHITE + Bukkit.getOfflinePlayer(uuid).getName());
                    });
                    lore.add("");
                    lore.add(ChatColor.GRAY + "ID: " + ChatColor.DARK_GRAY + session.getSessionId());
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Cliquez pour regarder !");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.addItem(item);
            }
        });

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Extraire l'ID de la session depuis le lore
        List<String> lore = meta.getLore();
        if (lore == null) return;
        
        String sessionId = "";
        for (String line : lore) {
            String plainLine = ChatColor.stripColor(line);
            if (plainLine.startsWith("ID: ")) {
                sessionId = plainLine.replace("ID: ", "").trim();
                break;
            }
        }

        if (sessionId.isEmpty()) return;

        GameSession session = ChaseTagLobby.getInstance().getGameManager().getSession(sessionId);
        if (session != null) {
            player.sendMessage(ChatColor.GREEN + "Connexion à la partie en tant que spectateur...");
            
            // Envoyer via Redis à Velocity
            try (redis.clients.jedis.Jedis jedis = ChaseTagLobby.getInstance().getJedisPool().getResource()) {
                jedis.publish(fr.rammex.chasetag.common.RedisChannel.SEND_TO_LOBBY,
                    player.getUniqueId().toString() + ":" + session.getPterodactylServerId());
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
