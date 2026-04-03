package fr.rammex.chaseTag.game.player;

public class Player {
    private final String playerUUID;
    private Role playerRole;
    private Rank playerRank;

    public Player(String playerUUID,Role playerRole, Rank playerRank){
        this.playerUUID = playerUUID;
        this.playerRole = playerRole;
        this.playerRank = playerRank;
    }

    public Rank getPlayerRank() {
        return playerRank;
    }

    public Role getPlayerRole() {
        return playerRole;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerRank(Rank playerRank) {
        this.playerRank = playerRank;
    }

    public void setPlayerRole(Role playerRole) {
        this.playerRole = playerRole;
    }
}
