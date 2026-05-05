package fr.rammex.chasetag.lobby.player.events;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.Player;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.PlayerMongoRepository;
import fr.rammex.chasetag.lobby.player.rank.Rank;
import fr.rammex.chasetag.lobby.utils.ColorUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerLobbyEvents implements Listener {
    private final PlayerMongoRepository playerMongoRepository;

    public PlayerLobbyEvents(PlayerMongoRepository playerMongoRepository) {
        this.playerMongoRepository = playerMongoRepository;
    }

    @EventHandler
    public void onPlayerMessage(AsyncChatEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        if (player1 != null && Boolean.TRUE.equals(player1.getPlayerData("muted"))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Vous êtes muet ! Raison : " + ChatColor.WHITE + player1.getPlayerData("mute_reason"));
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        Rank playerRole = player1.getPlayerRole();
        Component prefix;
        if(playerRole.getHex3() == ""){
            prefix = ColorUtils.gradient(playerRole.getPrefix()+" ", playerRole.getHex1(), playerRole.getHex2());
        } else {
            prefix = ColorUtils.gradient(playerRole.getPrefix()+" ", playerRole.getHex1(), playerRole.getHex2(), playerRole.getHex3());
        }

        Component messageColorized = ColorUtils.colorize("<#C2C2C2>"+player.getName()+" >> "+ message);

        Component fullMessage = Component.empty()
                .append(prefix)
                .append(messageColorized);

        Bukkit.broadcast(fullMessage);
    }

    @EventHandler
    public void onPlayerJoinFirstTime(PlayerJoinEvent event){
        org.bukkit.entity.Player player = event.getPlayer();

        // Whitelist check
        if (ChaseTagLobby.getInstance().getConfig().getBoolean("discord.whitelist-enabled", false)) {
            Player p = PlayerManager.getPlayer(player.getUniqueId().toString());
            if (p == null) {
                // Check in DB if not in memory
                p = playerMongoRepository.getPlayerByUUID(player.getUniqueId().toString()).orElse(null);
            }

            if (p == null || !((Boolean) p.getPlayerData().getOrDefault("whitelisted", false))) {
                player.kick(Component.text("§cVous n'êtes pas sur la whitelist.\n§7Rejoignez notre Discord pour vous faire whitelist !"));
                return;
            }
        }
        
        // Priorité au chargement depuis MongoDB pour avoir les stats fraîches
        Player stored = playerMongoRepository != null ? 
            playerMongoRepository.getPlayerByUUID(player.getUniqueId().toString()).orElse(null) : null;

        if (stored == null) {
            // Fallback sur le cache local ou nouveau joueur
            stored = PlayerManager.getPlayer(player.getUniqueId().toString());
            if (stored == null) {
                stored = new Player(player.getUniqueId().toString(), player.getName(), Rank.Joueur);
            }
        }

        if (stored != null && Boolean.TRUE.equals(stored.getPlayerData("banned"))) {
            player.kick(Component.text("§cVous êtes banni de ce serveur.\n§7Raison: §f" + stored.getPlayerData("ban_reason")));
            return;
        }

        // Toujours mettre à jour le nom si nécessaire
        if (!player.getName().equals(stored.getPlayerName())) {
            stored.setPlayerName(player.getName());
        }

        // Ajouter/Mettre à jour dans le manager local
        PlayerManager.addPlayer(stored);
        
        if (playerMongoRepository != null) {
            playerMongoRepository.savePlayer(stored);
        }

        // Redirection si une partie est en cours
        fr.rammex.chasetag.lobby.game.GameSession session = ChaseTagLobby.getInstance().getGameManager().getSessionByPlayer(player.getUniqueId());
        if (session != null && session.getStatus() == fr.rammex.chasetag.lobby.game.GameSession.Status.PLAYING) {
            String serverId = session.getPterodactylServerId();
            if (serverId != null) {
                player.sendMessage("§aReconnexion à votre partie en cours...");
                try (redis.clients.jedis.Jedis jedis = ChaseTagLobby.getInstance().getJedisPool().getResource()) {
                    jedis.publish(fr.rammex.chasetag.common.RedisChannel.SEND_TO_LOBBY,
                        player.getUniqueId().toString() + ":" + serverId);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = PlayerManager.getPlayer(event.getPlayer().getUniqueId().toString());
        if (player != null && playerMongoRepository != null) {
            playerMongoRepository.savePlayer(player);
        }
    }
}
