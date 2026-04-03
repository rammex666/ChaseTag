package fr.rammex.chaseTag.lobby.game;

import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.common.ServerState;
import fr.rammex.chaseTag.lobby.ChaseTagLobby;
import fr.rammex.chaseTag.lobby.pterodactyl.PterodactylClient;
import redis.clients.jedis.Jedis;

import java.util.*;

public class GameManager {

    private final ChaseTagLobby plugin;
    private final Map<String, GameSession> sessions = new HashMap<>();
    // playerUuid -> sessionId pour lookup rapide
    private final Map<UUID, String> playerSession = new HashMap<>();

    public GameManager(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    // Créer une nouvelle session (appelé par /chasetag create)
    public GameSession createSession(UUID ownerUuid) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(sessionId, ownerUuid);
        session.addPlayer(ownerUuid);
        sessions.put(sessionId, session);
        playerSession.put(ownerUuid, sessionId);
        plugin.getLogger().info("Session créée : " + sessionId + " par " + ownerUuid);
        return session;
    }

    // Rejoindre en tant que joueur (appelé par /chasetag join <id>)
    public boolean joinSession(String sessionId, UUID playerUuid) {
        GameSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.isFull()) return false;
        if (playerSession.containsKey(playerUuid)) return false;

        session.addPlayer(playerUuid);
        playerSession.put(playerUuid, sessionId);

        // Les 2 joueurs sont là → spawn le serveur
        if (session.isReady()) {
            spawnServer(session);
        }
        return true;
    }

    // Rejoindre en tant que spectateur (appelé par /chasetag spectate <id>)
    public boolean spectateSession(String sessionId, UUID spectatorUuid) {
        GameSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.getStatus() == GameSession.Status.WAITING) return false;

        session.addSpectator(spectatorUuid);
        playerSession.put(spectatorUuid, sessionId);
        return true;
    }

    // Spawn le serveur de jeu via Pterodactyl
    private void spawnServer(GameSession session) {
        session.setStatus(GameSession.Status.STARTING);

        // Async pour ne pas bloquer le thread principal
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PterodactylClient.ServerInfo info = plugin.getPterodactylClient()
                    .createServer(session.getSessionId());

                session.setPterodactylServerId(info.serverId());
                session.setPort(info.port());

                // Stocker dans Redis
                try (Jedis jedis = plugin.getJedisPool().getResource()) {
                    jedis.hset(RedisChannel.SERVERS_MAP, info.serverId(),
                               ServerState.STARTING.name());
                    jedis.hset(RedisChannel.SERVERS_PORT, info.serverId(),
                               String.valueOf(info.port()));
                }

                plugin.getLogger().info("Serveur spawné : " + info.serverId()
                    + " port=" + info.port());

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur spawn serveur : " + e.getMessage());
                session.setStatus(GameSession.Status.WAITING);
            }
        });
    }

    // Appelé par LobbyRedisListener quand GAME_END est reçu
    public void onGameEnd(String pterodactylServerId, List<UUID> players) {
        // Retrouver la session
        sessions.values().stream()
            .filter(s -> pterodactylServerId.equals(s.getPterodactylServerId()))
            .findFirst()
            .ifPresent(session -> {
                session.setStatus(GameSession.Status.ENDING);

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    // Kill le serveur Pterodactyl
                    try {
                        plugin.getPterodactylClient().deleteServer(pterodactylServerId);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Erreur kill serveur : " + e.getMessage());
                    }

                    // Cleanup Redis
                    try (Jedis jedis = plugin.getJedisPool().getResource()) {
                        jedis.hdel(RedisChannel.SERVERS_MAP, pterodactylServerId);
                        jedis.hdel(RedisChannel.SERVERS_PORT, pterodactylServerId);
                    }

                    // Cleanup mémoire
                    sessions.remove(session.getSessionId());
                    session.getPlayers().forEach(playerSession::remove);
                    session.getSpectators().forEach(playerSession::remove);
                });
            });
    }

    public GameSession getSession(String sessionId) { return sessions.get(sessionId); }
    public GameSession getSessionByPlayer(UUID uuid) {
        String id = playerSession.get(uuid);
        return id != null ? sessions.get(id) : null;
    }
    public Map<String, GameSession> getSessions() { return sessions; }
}