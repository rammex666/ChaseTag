package fr.rammex.chasetag.lobby.command;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReadyCommand implements CommandExecutor {

    private final ChaseTagLobby plugin;

    public ReadyCommand(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        var tournamentManager = plugin.getTournamentManager();
        var matchOpt = tournamentManager.getMatchForPlayer(player.getName());

        if (matchOpt.isPresent()) {
            var match = matchOpt.get();
            boolean currentReady = match.isPlayerReady(player.getName());
            boolean nextReady = !currentReady;
            
            tournamentManager.setPlayerReady(match.getPhase(), match.getPool(), match.getMatchId(), player.getName(), nextReady);
            
            player.sendMessage(ChatColor.GOLD + "[ChaseTag] " + ChatColor.YELLOW + "Ton statut est maintenant : " + (nextReady ? ChatColor.GREEN + "PRÊT" : ChatColor.RED + "PAS PRÊT"));
            
            // Notify opponent
            String opponentName = player.getName().equalsIgnoreCase(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
            if (opponentName != null) {
                Player opponent = Bukkit.getPlayerExact(opponentName);
                if (opponent != null) {
                    opponent.sendMessage(ChatColor.GOLD + "[ChaseTag] " + ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " est maintenant " + (nextReady ? ChatColor.GREEN + "PRÊT" : ChatColor.RED + "PAS PRÊT"));
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Tu n'as pas de match de tournoi assigné actuellement.");
        }

        return true;
    }
}
