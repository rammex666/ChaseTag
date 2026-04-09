package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.timer.Timer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private final ChaseTag plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardManager(ChaseTag plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
            updateActionBar(player);
        }
    }

    public void updateScoreboard(org.bukkit.entity.Player player) {
        GameManager gm = plugin.getGameManager();
        Game game = gm.getGame();
        if (game == null) return;

        Scoreboard scoreboard = scoreboards.computeIfAbsent(player.getUniqueId(), uuid -> {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(sb);
            return sb;
        });

        Objective objective = scoreboard.getObjective("chasetag");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("chasetag", Criteria.DUMMY, Component.text("§6§lCHASE TAG").decoration(TextDecoration.BOLD, true));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Player gamePlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId().toString());
        String roleStr = (gamePlayer != null) ? gamePlayer.getPlayerRole().getPrefix() : "§7Aucun";

        Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
        String timeStr = (timer != null) ? timer.getFormattedTimeRemaining() : "00:00";

        // Reset scores to update lines
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        objective.getScore("§7----------------").setScore(10);
        objective.getScore("§fManche: §e" + game.getCurrentManche() + "/" + game.getMaxManchesPerRound()).setScore(9);
        objective.getScore("§fRound: §e" + game.getCurrentRound() + "/" + game.getMaxRounds()).setScore(8);
        objective.getScore("  ").setScore(7);
        
        int i = 6;
        for (Player p : game.getPlayers()) {
            String name = (p.getBukkitPlayer() != null) ? p.getBukkitPlayer().getName() : "Inconnu";
            objective.getScore("§f" + name + ": §6" + game.getScore(p.getPlayerUUID())).setScore(i);
            i--;
        }

        objective.getScore(" ").setScore(4);
        objective.getScore("§fTemps: §a" + timeStr).setScore(2);
        objective.getScore("§fRôle: " + roleStr).setScore(1);
        objective.getScore("§7-----------------").setScore(0);
    }

    public void updateActionBar(org.bukkit.entity.Player player) {
        Game game = plugin.getGameManager().getGame();
        if (game == null) return;

        Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
        if (timer != null) {
            String timeStr = timer.getFormattedTimeRemaining();
            player.sendActionBar(Component.text("§fTemps restant: §a" + timeStr+ "§f ♛ Distance: §a"+getPlayersDistance(player)+"m", NamedTextColor.WHITE));
        }
    }

    private String getPlayersDistance(org.bukkit.entity.Player player){
        org.bukkit.entity.Player otherPlayer = null;
        for(Player p : plugin.getGameManager().getGame().getPlayers()){
            if(!p.getPlayerUUID().equals(player.getUniqueId().toString())){
                otherPlayer = p.getBukkitPlayer();
                break;
            }
        }

        if (otherPlayer == null) return "0";

        return String.valueOf((int) Math.round(player.getLocation().distance(otherPlayer.getLocation())));
    }

    public void removePlayer(org.bukkit.entity.Player player) {
        scoreboards.remove(player.getUniqueId());
    }
}
