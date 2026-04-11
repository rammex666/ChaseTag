package fr.rammex.chasetag.velocity.redis;

import fr.rammex.chasetag.common.MessageSerializer;
import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.common.message.GameEndMessage;
import fr.rammex.chasetag.common.message.ServerReadyMessage;
import fr.rammex.chasetag.velocity.ChaseTagVelocity;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class VelocityRedisListener {

    private final ChaseTagVelocity plugin;
    private JedisPubSub pubSub;
    private Thread listenerThread;

    public VelocityRedisListener(ChaseTagVelocity plugin) {
        this.plugin = plugin;
    }

    public void start() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equals(RedisChannel.SEND_TO_LOBBY)) {
                    // Format : "playerUuid:serverId"
                    // Ici on envoie vers un game server
                    handleSendToServer(message);
                } else if (channel.equals(RedisChannel.GAME_END)) {
                    // Renvoyer tous les joueurs vers le lobby
                    handleSendToLobby(message);
                // Dans onMessage, ajouter :
                } else if (channel.equals(RedisChannel.SERVER_READY)) {
                    handleServerReady(message);
                }
            }
        };

        listenerThread = new Thread(() -> {
            try (Jedis jedis = plugin.getJedisPool().getResource()) {
                jedis.subscribe(pubSub,
                    RedisChannel.SEND_TO_LOBBY,
                    RedisChannel.GAME_END,
                    RedisChannel.SERVER_READY
                );
            } catch (Exception e) {
                plugin.getLogger().error("Redis listener erreur : " + e.getMessage());
            }
        }, "chasetag-velocity-redis");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
    }

    // Envoyer un joueur vers un game server
    // message format : "playerUuid:serverName"
    private void handleSendToServer(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length != 2) return;

        String playerUuid = parts[0];
        String serverName = parts[1];

        plugin.getProxy().getPlayer(java.util.UUID.fromString(playerUuid))
            .ifPresent(player ->
                plugin.getProxy().getServer(serverName)
                    .ifPresent(server ->
                        player.createConnectionRequest(server).fireAndForget()
                    )
            );
    }

    // Renvoyer tous les joueurs d'un game server vers le lobby
    // message format : JSON GameEndMessage
    private void handleSendToLobby(String message) {
        // Deserialiser pour récupérer la liste des joueurs
        GameEndMessage msg =
            MessageSerializer.deserialize(
                message, GameEndMessage.class);

        String lobbyName = plugin.getConfig().getLobbyServerName();

        plugin.getProxy().getServer(lobbyName).ifPresent(lobby -> {
            msg.getPlayerUuids().forEach(uuidStr -> {
                plugin.getProxy().getPlayer(java.util.UUID.fromString(uuidStr))
                    .ifPresent(player ->
                        player.createConnectionRequest(lobby).fireAndForget()
                    );
            });
        });
    }

    private void handleServerReady(String message) {
                    ServerReadyMessage msg = MessageSerializer.deserialize(message, ServerReadyMessage.class);
                    String host = msg.getHost();
                    int port = msg.getPort();
                    String externalHost = plugin.getConfig().getExternalHost();
                    if (externalHost != null && !externalHost.isBlank()) {
                        if (host == null || host.isBlank()
                                || host.equalsIgnoreCase("localhost")
                                || host.equals("127.0.0.1")
                                || host.equals("0.0.0.0")) {
                            host = externalHost;
                        }
                    }

                    boolean usedFallbackPort = false;
                    if (port <= 0 || port == 25565) {
                        try (Jedis jedis = plugin.getJedisPool().getResource()) {
                            String storedPort = jedis.hget(RedisChannel.SERVERS_PORT, msg.getServerId());
                            if (storedPort != null && !storedPort.isBlank()) {
                                try {
                                    port = Integer.parseInt(storedPort);
                                    usedFallbackPort = true;
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warn("Impossible de récupérer le port réel depuis Redis pour " + msg.getServerId() + ": " + e.getMessage());
                        }
                    }

                    plugin.getLogger().info("SERVER_READY reçu: id=" + msg.getServerId() + ", host=" + msg.getHost() + ", port=" + msg.getPort() + ", resolvedHost=" + host + ", resolvedPort=" + port + ", fallbackPort=" + usedFallbackPort);
                    plugin.registerGameServer(msg.getServerId(), host, port);
                }
}
