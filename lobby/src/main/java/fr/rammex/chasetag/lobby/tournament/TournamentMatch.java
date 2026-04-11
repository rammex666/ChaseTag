package fr.rammex.chasetag.lobby.tournament;

import org.bson.Document;

public class TournamentMatch {
    private final String phase;
    private final int pool;
    private final int matchId;
    private final String player1;
    private final String player2;

    public TournamentMatch(String phase, int pool, int matchId, String player1, String player2) {
        this.phase = phase;
        this.pool = pool;
        this.matchId = matchId;
        this.player1 = player1 == null ? "" : player1;
        this.player2 = player2 == null ? "" : player2;
    }

    public String getPhase() {
        return phase;
    }

    public int getPool() {
        return pool;
    }

    public int getMatchId() {
        return matchId;
    }

    public String getPlayer1() {
        return player1 == null || player1.isBlank() ? null : player1;
    }

    public String getPlayer2() {
        return player2 == null || player2.isBlank() ? null : player2;
    }

    public String getPlayer1String() {
        return player1 == null || player1.isBlank() ? "Vide" : player1;
    }

    public String getPlayer2String() {
        return player2 == null || player2.isBlank() ? "Vide" : player2;
    }

    public boolean containsPlayer(String playerName) {
        if (playerName == null) {
            return false;
        }
        return playerName.equals(player1) || playerName.equals(player2);
    }

    public Document toDocument() {
        return new Document("phase", phase)
                .append("pool", pool)
                .append("matchId", matchId)
                .append("player1", player1)
                .append("player2", player2);
    }

    public static TournamentMatch fromDocument(Document document) {
        if (document == null) {
            return null;
        }
        return new TournamentMatch(
                document.getString("phase"),
                document.getInteger("pool", 0),
                document.getInteger("matchId", 0),
                document.getString("player1"),
                document.getString("player2")
        );
    }
}
