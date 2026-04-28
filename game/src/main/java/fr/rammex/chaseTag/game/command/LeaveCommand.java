package fr.rammex.chaseTag.game.command;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.game.Game;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.PlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LeaveCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) sender;
        Player player = PlayerManager.getPlayer(bukkitPlayer.getUniqueId().toString());

        if (player == null) return true;

        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game == null) {
            bukkitPlayer.sendMessage(ChatColor.RED + "Il n'y a aucune partie en cours.");
            return true;
        }

        if (!game.getPlayers().contains(player)) {
            bukkitPlayer.sendMessage(ChatColor.RED + "Vous ne participez pas à la partie actuelle.");
            return true;
        }

        ChaseTag.getInstance().getGameManager().forceEndGame(player);
        return true;
    }
}
