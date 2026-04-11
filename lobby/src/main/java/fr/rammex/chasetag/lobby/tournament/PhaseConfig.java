package fr.rammex.chasetag.lobby.tournament;

public class PhaseConfig {
    private final String name;
    private final PhaseType type;
    private final int pouleCount;
    private final int playersPerPoule;
    private final int bracketSize;

    public PhaseConfig(String name, PhaseType type, int pouleCount, int playersPerPoule, int bracketSize) {
        this.name = name;
        this.type = type;
        this.pouleCount = pouleCount;
        this.playersPerPoule = playersPerPoule;
        this.bracketSize = bracketSize;
    }

    public String getName() {
        return name;
    }

    public PhaseType getType() {
        return type;
    }

    public int getPouleCount() {
        return pouleCount;
    }

    public int getPlayersPerPoule() {
        return playersPerPoule;
    }

    public int getBracketSize() {
        return bracketSize;
    }

    public boolean isPoule() {
        return type == PhaseType.POULE;
    }

    public boolean isBracket() {
        return type == PhaseType.BRACKET;
    }
}
