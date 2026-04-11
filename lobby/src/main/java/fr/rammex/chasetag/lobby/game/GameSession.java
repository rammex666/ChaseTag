package fr.rammex.chasetag.lobby.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSession {

    public enum Status { WAITING, STARTING, PLAYING, ENDING }

    private final String sessionId;
    private final UUID ownerUuid;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private String pterodactylServerId;
    private int port;
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
    public String getPterodactylServerId() { return pterodactylServerId; }
    public void setPterodactylServerId(String id) { this.pterodactylServerId = id; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}