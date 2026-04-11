package fr.rammex.chasetag.lobby.duel;

import java.util.HashMap;
import java.util.Map;

public class DuelRequestManager {
    private final Map<String, String> pendingRequests = new HashMap<>();

    public boolean hasPendingRequestFor(String targetName) {
        return pendingRequests.containsKey(targetName);
    }

    public String getRequesterFor(String targetName) {
        return pendingRequests.get(targetName);
    }

    public void createRequest(String requesterName, String targetName) {
        pendingRequests.put(targetName, requesterName);
    }

    public void removeRequest(String targetName) {
        pendingRequests.remove(targetName);
    }

    public void cancelRequest(String requesterName) {
        pendingRequests.values().removeIf(value -> value.equals(requesterName));
    }
}
