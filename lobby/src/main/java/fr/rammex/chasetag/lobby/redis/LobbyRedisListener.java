package fr.rammex.chasetag.lobby.redis;

import fr.rammex.chasetag.common.MessageSerializer;
import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.common.ServerState;
import fr.rammex.chasetag.common.message.GameEndMessage;
import fr.rammex.chasetag.common.message.ServerReadyMessage;
import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.game.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.stream.Collectors;

public class LobbyRedisListener {

    private final ChaseTagLobby plugin;
    private JedisPubSub pubSub;
    private Thread listenerThread;

    public LobbyRedisListener(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    public void start() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equals(RedisChannel.SERVER_READY)) {
                    handleServerReady(message);
                } else if (channel.equals(RedisChannel.GAME_END)) {
                    handleGameEnd(message);
                }
            }
        };

        listenerThread = new Thread(() -> {
            try (Jedis jedis = plugin.getJedisPool().getResource()) {
                jedis.subscribe(pubSub,
                    RedisChannel.SERVER_READY,
                    RedisChannel.GAME_END
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Redis listener erreur : " + e.getMessage());
            }
        }, "chasetag-redis-listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
    }

    private void handleServerReady(String json) {
        ServerReadyMessage msg = MessageSerializer.deserialize(json,
            ServerReadyMessage.class);

        // Mettre à jour Redis
        try (Jedis jedis = plugin.getJedisPool().getResource()) {
            jedis.hset(RedisChannel.SERVERS_MAP, msg.getServerId(),
                ServerState.WAITING.name());
        }

        // Retrouver la session et envoyer les joueurs (sur le thread principal)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getGameManager().getSessions().values().stream()
                .filter(s -> msg.getServerId().equals(s.getPterodactylServerId()))
                .findFirst()
                .ifPresent(session -> {
                    session.setStatus(GameSession.Status.PLAYING);
                    sendPlayersToServer(session, msg.getServerId());

                    // Envoyer le log sur Discord
                    if (plugin.getDiscordBot() != null) {
                        String p1 = "Inconnu";
                        String p2 = "Inconnu";
                        if (session.getPlayers().size() >= 1) {
                            p1 = Bukkit.getOfflinePlayer(session.getPlayers().get(0)).getName();
                        }
                        if (session.getPlayers().size() >= 2) {
                            p2 = Bukkit.getOfflinePlayer(session.getPlayers().get(1)).getName();
                        }
                        plugin.getDiscordBot().sendGameStartLog(p1, p2, session.getMapName());
                    }
                });
        });
    }

    private void handleGameEnd(String json) {
        GameEndMessage msg = MessageSerializer.deserialize(json, GameEndMessage.class);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("GAME_END reçu: serverId=" + msg.getServerId());
            
            GameSession session = plugin.getGameManager().getSessions().values().stream()
                    .filter(s -> msg.getServerId().equals(s.getPterodactylServerId()))
                    .findFirst()
                    .orElse(null);

            if (session == null) {
                plugin.getLogger().warning("Session introuvable pour le serveur " + msg.getServerId());
            }

            System.out.println("Partie terminée sur le serveur " + msg.getServerId() + ", gagnant : " + msg.getWinnerName());
            if (msg.getWinnerName() != null && !msg.getWinnerName().equalsIgnoreCase("Aucun")) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[ChaseTag] " + ChatColor.AQUA + msg.getWinnerName() + 
                    ChatColor.YELLOW + " a gagné sa partie sur le serveur " + ChatColor.WHITE + msg.getServerId() + " !");
            }

            // Sauvegarder les stats dans MongoDB
            if (msg.getPlayerScores() != null) {
                String gameType = (session != null) ? session.getType().name() : "DUEL";
                
                for (java.util.Map.Entry<String, Integer> entry : msg.getPlayerScores().entrySet()) {
                    String uuidStr = entry.getKey();
                    int score = entry.getValue();
                    boolean isWinner = uuidStr.equals(msg.getWinnerUuid());

                    plugin.getPlayerMongoRepository().getPlayerByUUID(uuidStr).ifPresent(player -> {
                        player.incrementGamesPlayed();
                        if (isWinner) {
                            player.incrementWins();
                        } else {
                            player.incrementLosses();
                        }
                        player.updateBestScore(gameType, score);
                        
                        plugin.getPlayerMongoRepository().savePlayer(player);
                        plugin.getLogger().info("Stats sauvegardées pour " + player.getPlayerName() + " (" + gameType + ")");

                        // Reset readiness for tournament matches
                        plugin.getTournamentManager().resetPlayerReady(player.getPlayerName());
                        org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(player.getPlayerName());
                        if (onlinePlayer != null) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                // Refresh the ready item in their inventory
                                new fr.rammex.chasetag.lobby.listener.LobbyListener(plugin).updateReadyItem(onlinePlayer);
                            });
                        }
                    });
                }
            }

            // Sauvegarder les points globaux dans Redis
            if (msg.getPlayerScores() != null) {
                try (Jedis jedis = plugin.getJedisPool().getResource()) {
                    for (java.util.Map.Entry<String, Integer> entry : msg.getPlayerScores().entrySet()) {
                        jedis.hincrBy(RedisChannel.GLOBAL_POINTS, entry.getKey(), entry.getValue());
                    }
                }
            }

            plugin.getGameManager().onGameEnd(
                msg.getServerId(),
                msg.getPlayerUuids().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList())
            );
        });
    }

    // Publie sur Redis pour que Velocity envoie les joueurs
    private void sendPlayersToServer(GameSession session, String serverId) {
        try (Jedis jedis = plugin.getJedisPool().getResource()) {
            for (UUID uuid : session.getPlayers()) {
                jedis.publish(RedisChannel.SEND_TO_LOBBY,
                    uuid + ":" + serverId);
            }
        }
    }
}