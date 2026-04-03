package fr.rammex.chaseTag.game.redis;

import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chaseTag.game.ChaseTag;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class GameRedisListener {

    private final ChaseTag plugin;
    private JedisPubSub pubSub;
    private Thread listenerThread;

    public GameRedisListener(ChaseTag plugin) {
        this.plugin = plugin;
    }

    public void start() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                // Format : "playerUuid:serverId"
                // Le lobby nous envoie un joueur spectateur
                if (channel.equals(RedisChannel.SEND_TO_LOBBY)) {
                    String[] parts = message.split(":", 2);
                    if (parts.length != 2) return;

                    // Vérifier que ce message nous concerne
                    if (!parts[1].equals(plugin.getServerId())) return;

                    // Traiter sur le thread principal Bukkit
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // Le joueur arrive → logique d'accueil spectateur si besoin
                        plugin.getLogger().info("Joueur entrant (spectateur) : " + parts[0]);
                    });
                }
            }
        };

        listenerThread = new Thread(() -> {
            try (Jedis jedis = plugin.getJedisPool().getResource()) {
                jedis.subscribe(pubSub, RedisChannel.SEND_TO_LOBBY);
            } catch (Exception e) {
                plugin.getLogger().severe("Redis listener erreur : " + e.getMessage());
            }
        }, "chasetag-game-redis");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
    }
}