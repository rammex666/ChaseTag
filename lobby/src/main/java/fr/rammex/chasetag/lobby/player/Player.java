package fr.rammex.chasetag.lobby.player;

import fr.rammex.chasetag.lobby.player.rank.Rank;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;

public class Player {
    private final String playerUUID;
    private String playerName;
    private Rank playerRank;
    private Map<String, Object> playerData;

    public Player(String playerUUID, String playerName, Rank playerRank) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.playerRank = playerRank;
        this.playerData = new java.util.HashMap<>();
    }

    public Rank getPlayerRole() {
        return playerRank;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerRole(Rank playerRank) {
        this.playerRank = playerRank;
    }

    public Map<String, Object> getPlayerData() {
        return playerData;
    }

    public Object getPlayerData(String key) {
        return playerData.get(key);
    }

    public void setPlayerData(String key, Object value) {
        this.playerData.put(key, value);
    }

    public void setPlayerData(Map<String, Object> playerData) {
        this.playerData = playerData;
    }

    public void incrementWins() {
        int wins = getIntData("wins") + 1;
        setPlayerData("wins", wins);
    }

    public void incrementLosses() {
        int losses = getIntData("losses") + 1;
        setPlayerData("losses", losses);
    }

    public void incrementGamesPlayed() {
        int games = getIntData("games_played") + 1;
        setPlayerData("games_played", games);
    }

    public void addPoints(int points) {
        int currentPoints = getIntData("points") + points;
        setPlayerData("points", currentPoints);
    }

    public int getPoints() {
        return getIntData("points");
    }

    public void updateBestScore(String type, int score) {
        String key = "best_score_" + type.toLowerCase();
        int currentBest = getIntData(key);
        if (score > currentBest) {
            setPlayerData(key, score);
        }
    }

    public void updateBestHunterTime(int time) {
        String key = "best_hunter_time";
        int currentBest = getIntData(key);
        // On initialise à 0, donc si currentBest est 0, n'importe quel temps est meilleur.
        // Sinon, on veut le temps le plus court.
        if (currentBest == 0 || time < currentBest) {
            setPlayerData(key, time);
        }
    }

    public void updateBestRunnerTime(int time) {
        String key = "best_runner_time";
        int currentBest = getIntData(key);
        // On veut le temps le plus long
        if (time > currentBest) {
            setPlayerData(key, time);
        }
    }

    public int getWins() {
        return getIntData("wins");
    }

    public int getLosses() {
        return getIntData("losses");
    }

    public int getGamesPlayed() {
        return getIntData("games_played");
    }

    public int getBestScore(String type) {
        return getIntData("best_score_" + type.toLowerCase());
    }

    public int getBestHunterTime() {
        return getIntData("best_hunter_time");
    }

    public int getBestRunnerTime() {
        return getIntData("best_runner_time");
    }

    private int getIntData(String key) {
        Object val = playerData.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return Bukkit.getPlayer(UUID.fromString(playerUUID));
    }
}
