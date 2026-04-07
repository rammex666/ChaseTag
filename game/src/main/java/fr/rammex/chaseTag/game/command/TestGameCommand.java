package fr.rammex.chaseTag.game.command;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.arena.Arena;
import fr.rammex.chaseTag.game.game.Game;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestGameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Seulement les administrateurs peuvent utiliser cette commande.");
            return true;
        }

        if (ChaseTag.getInstance().getGameManager().getGame() != null) {
            sender.sendMessage(ChatColor.RED + "Une partie est déjà en cours ou en préparation.");
            return true;
        }

        Arena arena = ChaseTag.getInstance().getArenaManager().getAll().values().stream().findFirst().orElse(null);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Aucune arène n'est configurée !");
            return true;
        }

        List<Player> allOnline = Bukkit.getOnlinePlayers().stream()
                .map(p -> PlayerManager.getPlayer(p.getUniqueId().toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (allOnline.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Il n'y a aucun joueur en ligne pour lancer le test.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Lancement de la partie de test sur l'arène : " + arena.getName());

        Game game = new Game(ChaseTag.getInstance().getServerId(), arena);
        
        // On prend jusqu'à 2 joueurs actifs, le reste en spectateur
        List<Player> activePlayers = allOnline.stream().limit(2).collect(Collectors.toList());
        List<Player> spectators = allOnline.stream().skip(2).collect(Collectors.toList());
        
        game.setPlayers(activePlayers);
        game.setSpectators(spectators);
        
        ChaseTag.getInstance().getGameManager().initGame(game);
        ChaseTag.getInstance().getGameManager().startGame();

        return true;
    }
}
