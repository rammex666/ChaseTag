package fr.rammex.chasetag.lobby.game;

import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.common.ServerState;
import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.pterodactyl.PterodactylClient;
import redis.clients.jedis.Jedis;

import java.util.*;

public class GameManager {

    private final ChaseTagLobby plugin;
    private final Map<String, GameSession> sessions = new HashMap<>();
    private final Map<UUID, String> playerSession = new HashMap<>();

    public GameManager(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    public GameSession createSession(UUID ownerUuid) {
        return createSession(ownerUuid, GameSession.GameType.DUEL);
    }

    public GameSession createSession(UUID ownerUuid, GameSession.GameType type) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(sessionId, ownerUuid, type);
        session.addPlayer(ownerUuid);
        sessions.put(sessionId, session);
        playerSession.put(ownerUuid, sessionId);
        plugin.getLogger().info("Session créée : " + sessionId + " par " + ownerUuid + " (Type: " + type + ")");
        return session;
    }

    public void createSoloSession(UUID playerUuid, int eggId, String mapName) {
        if (getSessionByPlayer(playerUuid) != null) return;
        GameSession session = createSession(playerUuid, GameSession.GameType.DUEL);
        session.setMap(eggId, mapName);
        session.setStatus(GameSession.Status.STARTING);
        spawnServer(session);
    }

    public boolean joinSession(String sessionId, UUID playerUuid) {
        GameSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.isFull()) return false;
        if (playerSession.containsKey(playerUuid)) return false;

        session.addPlayer(playerUuid);
        playerSession.put(playerUuid, sessionId);

        if (session.isReady()) {
            spawnServer(session);
        }
        return true;
    }

    public boolean spectateSession(String sessionId, UUID spectatorUuid) {
        GameSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.getStatus() == GameSession.Status.WAITING) return false;

        session.addSpectator(spectatorUuid);
        playerSession.put(spectatorUuid, sessionId);
        return true;
    }

    private void spawnServer(GameSession session) {
        session.setStatus(GameSession.Status.STARTING);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PterodactylClient.ServerInfo info = plugin.getPterodactylClient()
                        .createServer(session.getSessionId(), session.getEggId());

                session.setPterodactylInternalId(info.serverId());
                session.setPterodactylNumericId(info.numericId());
                session.setPort(info.port());

                try (Jedis jedis = plugin.getJedisPool().getResource()) {
                    jedis.hset(RedisChannel.SERVERS_MAP, info.serverId(),
                            ServerState.STARTING.name());
                    jedis.hset(RedisChannel.SERVERS_PORT, info.serverId(),
                            String.valueOf(info.port()));
                }

                plugin.getLogger().info("Serveur spawné : " + info.serverId()
                        + " numericId=" + info.numericId()
                        + " port=" + info.port());

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur spawn serveur : " + e.getMessage());
                session.setStatus(GameSession.Status.WAITING);
            }
        });
    }

    public void onGameEnd(String pterodactylServerId, List<UUID> players) {
        sessions.values().stream()
                .filter(s -> pterodactylServerId.equals(s.getPterodactylServerId()))
                .findFirst()
                .ifPresent(session -> {
                    session.setStatus(GameSession.Status.ENDING);

                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            plugin.getLogger().info("Suppression du serveur #" + session.getPterodactylNumericId());
                            plugin.getPterodactylClient().deleteServer(session.getPterodactylNumericId());
                        } catch (Exception e) {
                            plugin.getLogger().severe("Erreur kill serveur : " + e.getMessage());
                        }

                        try (Jedis jedis = plugin.getJedisPool().getResource()) {
                            jedis.hdel(RedisChannel.SERVERS_MAP, session.getPterodactylInternalId());
                            jedis.hdel(RedisChannel.SERVERS_PORT, session.getPterodactylInternalId());
                        }

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