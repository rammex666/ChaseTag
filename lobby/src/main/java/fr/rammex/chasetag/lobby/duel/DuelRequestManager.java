package fr.rammex.chasetag.lobby.duel;

import java.util.HashMap;
import java.util.Map;

public class DuelRequestManager {
    private final Map<String, DuelRequest> pendingRequests = new HashMap<>();

    public boolean hasPendingRequestFor(String targetName) {
        return pendingRequests.containsKey(targetName);
    }

    public DuelRequest getRequestFor(String targetName) {
        return pendingRequests.get(targetName);
    }

    public void createRequest(String requesterName, String targetName, int eggId, String mapName) {
        pendingRequests.put(targetName, new DuelRequest(requesterName, targetName, eggId, mapName));
    }

    public void removeRequest(String targetName) {
        pendingRequests.remove(targetName);
    }

    public void cancelRequest(String requesterName) {
        pendingRequests.values().removeIf(value -> value.getRequesterName().equals(requesterName));
    }
}
