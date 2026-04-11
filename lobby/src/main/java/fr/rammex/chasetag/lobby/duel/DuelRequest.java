package fr.rammex.chasetag.lobby.duel;

public class DuelRequest {
    private final String requesterName;
    private final String targetName;
    private final int eggId;
    private final String mapName;

    public DuelRequest(String requesterName, String targetName, int eggId, String mapName) {
        this.requesterName = requesterName;
        this.targetName = targetName;
        this.eggId = eggId;
        this.mapName = mapName == null ? "" : mapName;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getTargetName() {
        return targetName;
    }

    public int getEggId() {
        return eggId;
    }

    public String getMapName() {
        return mapName;
    }
}
