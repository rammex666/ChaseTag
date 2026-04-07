package fr.rammex.chaseTag.game.command;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.game.Game;
import fr.rammex.chaseTag.game.game.GameState;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.Role;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RoleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) sender;
        if (!bukkitPlayer.isOp()) {
            bukkitPlayer.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        Player gamePlayer = PlayerManager.getPlayer(bukkitPlayer.getUniqueId().toString());
        if (gamePlayer == null) return true;

        Game game = ChaseTag.getInstance().getGameManager().getGame();

        switch (label.toLowerCase()) {
            case "setplayer":
                setPlayer(bukkitPlayer, gamePlayer, game);
                break;
            case "setspec":
                setSpec(bukkitPlayer, gamePlayer, game);
                break;
            case "setstaff":
                setStaff(bukkitPlayer, gamePlayer, game);
                break;
        }

        return true;
    }

    private void setPlayer(org.bukkit.entity.Player bukkitPlayer, Player gamePlayer, Game game) {
        gamePlayer.setPlayerRole(Role.None);
        bukkitPlayer.setGameMode(GameMode.SURVIVAL);
        bukkitPlayer.sendMessage(ChatColor.GREEN + "Vous êtes maintenant un Joueur.");

        if (game != null) {
            game.getSpectators().remove(gamePlayer);
            if (!game.getPlayers().contains(gamePlayer)) {
                game.getPlayers().add(gamePlayer);
            }
            bukkitPlayer.teleport(game.getArena().getBlueSpawn());
        }
    }

    private void setSpec(org.bukkit.entity.Player bukkitPlayer, Player gamePlayer, Game game) {
        gamePlayer.setPlayerRole(Role.Spec);
        bukkitPlayer.setGameMode(GameMode.SPECTATOR);
        bukkitPlayer.sendMessage(ChatColor.GRAY + "Vous êtes maintenant un Spectateur.");

        if (game != null) {
            game.getPlayers().remove(gamePlayer);
            if (!game.getSpectators().contains(gamePlayer)) {
                game.getSpectators().add(gamePlayer);
            }
            bukkitPlayer.teleport(game.getArena().getSpecSpawn());
        }
    }

    private void setStaff(org.bukkit.entity.Player bukkitPlayer, Player gamePlayer, Game game) {
        gamePlayer.setPlayerRole(Role.Staff);
        bukkitPlayer.setGameMode(GameMode.SPECTATOR);
        bukkitPlayer.sendMessage(ChatColor.AQUA + "Vous êtes maintenant en mode Staff.");

        if (game != null) {
            game.getPlayers().remove(gamePlayer);
            if (!game.getSpectators().contains(gamePlayer)) {
                game.getSpectators().add(gamePlayer);
            }
            bukkitPlayer.teleport(game.getArena().getSpecSpawn());
        }
    }
}
