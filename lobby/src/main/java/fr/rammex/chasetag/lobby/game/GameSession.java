package fr.rammex.chasetag.lobby.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSession {

    public enum Status { WAITING, STARTING, PLAYING, ENDING }
    public enum GameType { DUEL, TOURNAMENT }

    private String sessionId;
    private final UUID ownerUuid;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private String pterodactylInternalId;
    private int pterodactylNumericId;
    private int port;
    private int eggId = -1;
    private String mapName = "";
    private Status status = Status.WAITING;
    private GameType type = GameType.DUEL;

    public GameSession(String sessionId, UUID ownerUuid) {
        this.sessionId = sessionId;
        this.ownerUuid = ownerUuid;
    }

    public GameSession(String sessionId, UUID ownerUuid, GameType type) {
        this.sessionId = sessionId;
        this.ownerUuid = ownerUuid;
        this.type = type;
    }

    public boolean isFull() { return players.size() >= 2; }
    public boolean isReady() { return players.size() == 2; }

    public boolean addPlayer(UUID uuid) {
        if (isFull()) return false;
        players.add(uuid);
        return true;
    }

    public void addSpectator(UUID uuid) { spectators.add(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }
    public void removeSpectator(UUID uuid) { spectators.remove(uuid); }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public List<UUID> getPlayers() { return players; }
    public List<UUID> getSpectators() { return spectators; }

    public String getPterodactylServerId() { return sessionId; }

    public String getPterodactylInternalId() { return pterodactylInternalId; }
    public void setPterodactylInternalId(String id) { this.pterodactylInternalId = id; }

    public int getPterodactylNumericId() { return pterodactylNumericId; }
    public void setPterodactylNumericId(int id) { this.pterodactylNumericId = id; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getEggId() { return eggId; }
    public String getMapName() { return mapName; }
    public void setMap(int eggId, String mapName) {
        this.eggId = eggId;
        this.mapName = mapName == null ? "" : mapName;
    }
    public boolean hasMap() { return eggId > 0; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public GameType getType() { return type; }
    public void setType(GameType type) { this.type = type; }
}