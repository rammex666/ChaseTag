package fr.rammex.chaseTag.lobby.command;

import fr.rammex.chaseTag.lobby.ChaseTagLobby;
import fr.rammex.chaseTag.lobby.game.GameSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayCommand implements CommandExecutor {

    private final ChaseTagLobby plugin;

    public PlayCommand(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage: /chasetag <create|join <id>|spectate <id>|list>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /chasetag join <id>");
                    return true;
                }
                handleJoin(player, args[1]);
            }
            case "spectate" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /chasetag spectate <id>");
                    return true;
                }
                handleSpectate(player, args[1]);
            }
            case "list" -> handleList(player);
            default -> player.sendMessage("§cSous-commande inconnue.");
        }
        return true;
    }

    private void handleCreate(Player player) {
        if (plugin.getGameManager().getSessionByPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§cTu es déjà dans une partie.");
            return;
        }
        GameSession session = plugin.getGameManager().createSession(player.getUniqueId());
        player.sendMessage("§aPartie créée ! ID: §e" + session.getSessionId());
        player.sendMessage("§7En attente d'un adversaire...");
    }

    private void handleJoin(Player player, String sessionId) {
        if (plugin.getGameManager().getSessionByPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§cTu es déjà dans une partie.");
            return;
        }
        boolean joined = plugin.getGameManager().joinSession(sessionId,
            player.getUniqueId());
        if (!joined) {
            player.sendMessage("§cPartie introuvable ou complète.");
            return;
        }
        player.sendMessage("§aPartie rejointe ! Serveur en cours de démarrage...");
    }

    private void handleSpectate(Player player, String sessionId) {
        boolean joined = plugin.getGameManager().spectateSession(sessionId,
            player.getUniqueId());
        if (!joined) {
            player.sendMessage("§cPartie introuvable ou pas encore démarrée.");
            return;
        }
        player.sendMessage("§aMode spectateur activé !");
    }

    private void handleList(Player player) {
        if (plugin.getGameManager().getSessions().isEmpty()) {
            player.sendMessage("§7Aucune partie en cours.");
            return;
        }
        player.sendMessage("§eParties disponibles :");
        plugin.getGameManager().getSessions().values().forEach(session ->
            player.sendMessage("§7- §e" + session.getSessionId()
                + " §7| " + session.getPlayers().size() + "/2 joueurs"
                + " | " + session.getStatus().name())
        );
    }
}