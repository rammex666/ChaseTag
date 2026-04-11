package fr.rammex.chasetag.lobby.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSession {

    public enum Status { WAITING, STARTING, PLAYING, ENDING }

    private String sessionId;
    private final UUID ownerUuid;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private String pterodactylInternalId; // identifier Pterodactyl (pour delete)
    private int port;
    private int eggId = -1;
    private String mapName = "";
    private Status status = Status.WAITING;

    public GameSession(String sessionId, UUID ownerUuid) {
        this.sessionId = sessionId;
        this.ownerUuid = ownerUuid;
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
    public UUID getOwnerUuid() { return ownerUuid; }
    public List<UUID> getPlayers() { return players; }
    public List<UUID> getSpectators() { return spectators; }

    // Pour le matching Redis — on utilise le sessionId directement
    public String getPterodactylServerId() { return sessionId; }

    // Identifier Pterodactyl (ex: "75ff9a73") — uniquement pour l'API delete/start
    public String getPterodactylInternalId() { return pterodactylInternalId; }
    public void setPterodactylInternalId(String id) { this.pterodactylInternalId = id; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

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
}