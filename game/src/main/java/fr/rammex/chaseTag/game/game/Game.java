package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.arena.Arena;
import fr.rammex.chaseTag.game.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    private final String gameServerID;
    private final Arena arena;
    private GameState gameState;
    private List<Player> players;
    private List<Player> spectators;

    private int currentRound = 1;
    private int currentManche = 1; // 1 ou 2 (Tour de chaque joueur)
    private int maxRounds = 4;
    private int maxManchesPerRound = 2;

    private Player currentChaser;
    private Player currentRunner;
    private boolean isCountdown = false;
    private boolean isTestDev = false;

    private final Map<String, Integer> playerScores = new HashMap<>(); // UUID -> Score

    public Game(String gameServerID, Arena arena){
        this.gameServerID = gameServerID;
        this.arena = arena;
        this.gameState = GameState.WAITING;
        this.players = new ArrayList<>();
        this.spectators = new ArrayList<>();
    }

    public boolean isTestDev() {
        return isTestDev;
    }

    public void setTestDev(boolean testDev) {
        isTestDev = testDev;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Arena getArena() {
        return arena;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Player> getSpectators() {
        return spectators;
    }

    public String getGameServerID() {
        return gameServerID;
    }

    public int getCurrentManche() {
        return currentManche;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentManche(int currentManche) {
        this.currentManche = currentManche;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public Player getCurrentChaser() {
        return currentChaser;
    }

    public void setCurrentChaser(Player currentChaser) {
        this.currentChaser = currentChaser;
    }

    public Player getCurrentRunner() {
        return currentRunner;
    }

    public void setCurrentRunner(Player currentRunner) {
        this.currentRunner = currentRunner;
    }

    public boolean isCountdown() {
        return isCountdown;
    }

    public void setCountdown(boolean isCountdown) {
        this.isCountdown = isCountdown;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
        for (Player player : players) {
            playerScores.put(player.getPlayerUUID(), 0);
        }
    }

    public void setSpectators(List<Player> spectators) {
        this.spectators = spectators;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public int getMaxManchesPerRound() {
        return maxManchesPerRound;
    }

    public void addScore(String playerUUID, int amount) {
        playerScores.put(playerUUID, playerScores.getOrDefault(playerUUID, 0) + amount);
    }

    public void incrementScore(String playerUUID) {
        addScore(playerUUID, 1);
    }

    public int getScore(String playerUUID) {
        return playerScores.getOrDefault(playerUUID, 0);
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }
}
