package fr.rammex.chasetag.lobby.command;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.duel.DuelRequestManager;
import fr.rammex.chasetag.lobby.game.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {
    private final ChaseTagLobby plugin;
    private final DuelRequestManager requestManager;

    public DuelCommand(ChaseTagLobby plugin, DuelRequestManager requestManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "request" -> handleRequest(player, args);
            case "accept" -> handleAccept(player);
            case "decline" -> handleDecline(player);
            case "cancel" -> handleCancel(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleRequest(Player player, String[] args) {
        if (!ensurePracticeMode(player)) return;
        if (args.length < 2) {
            player.sendMessage("§cUsage: /duel request <joueur>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cJoueur introuvable.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cVous ne pouvez pas vous défier vous-même.");
            return;
        }
        if (plugin.getGameManager().getSessionByPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§cVous êtes déjà dans une partie.");
            return;
        }
        if (plugin.getGameManager().getSessionByPlayer(target.getUniqueId()) != null) {
            player.sendMessage("§cCe joueur est déjà dans une partie.");
            return;
        }
        if (requestManager.hasPendingRequestFor(target.getName())) {
            player.sendMessage("§cCe joueur a déjà une demande en attente.");
            return;
        }

        requestManager.createRequest(player.getName(), target.getName());
        player.sendMessage("§aDemande de duel envoyée à §e" + target.getName() + "§a.");
        target.sendMessage("§e" + player.getName() + "§a te propose un duel. Tape §e/duel accept§a ou §e/duel decline§a.");
    }

    private void handleAccept(Player player) {
        if (!ensurePracticeMode(player)) return;
        String requesterName = requestManager.getRequesterFor(player.getName());
        if (requesterName == null) {
            player.sendMessage("§cAucune demande de duel en attente.");
            return;
        }

        Player requester = Bukkit.getPlayerExact(requesterName);
        if (requester == null) {
            player.sendMessage("§cLe joueur qui a demandé le duel n'est plus en ligne.");
            requestManager.removeRequest(player.getName());
            return;
        }
        if (plugin.getGameManager().getSessionByPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§cVous êtes déjà dans une partie.");
            requestManager.removeRequest(player.getName());
            return;
        }
        if (plugin.getGameManager().getSessionByPlayer(requester.getUniqueId()) != null) {
            player.sendMessage("§cLe joueur qui a demandé le duel est déjà dans une partie.");
            requestManager.removeRequest(player.getName());
            return;
        }

        GameSession session = plugin.getGameManager().createSession(requester.getUniqueId());
        boolean joined = plugin.getGameManager().joinSession(session.getSessionId(), player.getUniqueId());
        if (!joined) {
            player.sendMessage("§cImpossible de rejoindre la partie.");
            requester.sendMessage("§cLa demande de duel n'a pas pu être acceptée.");
            requestManager.removeRequest(player.getName());
            return;
        }

        requestManager.removeRequest(player.getName());
        requester.sendMessage("§aVotre duel avec §e" + player.getName() + "§a est accepté ! Serveur en cours de démarrage...");
        player.sendMessage("§aDuel accepté ! Serveur en cours de démarrage...");
    }

    private void handleDecline(Player player) {
        if (!ensurePracticeMode(player)) return;
        if (!requestManager.hasPendingRequestFor(player.getName())) {
            player.sendMessage("§cAucune demande de duel à refuser.");
            return;
        }

        String requesterName = requestManager.getRequesterFor(player.getName());
        requestManager.removeRequest(player.getName());
        player.sendMessage("§eDemande de duel refusée.");
        Player requester = Bukkit.getPlayerExact(requesterName);
        if (requester != null) {
            requester.sendMessage("§cVotre demande de duel a été refusée par §e" + player.getName() + "§c.");
        }
    }

    private void handleCancel(Player player) {
        if (!ensurePracticeMode(player)) return;
        requestManager.cancelRequest(player.getName());
        player.sendMessage("§eTous vos défis en attente ont été annulés.");
    }

    private void sendUsage(Player player) {
        player.sendMessage("§e/duel request <joueur> §7- Envoyer une demande de duel");
        player.sendMessage("§e/duel accept §7- Accepter une demande de duel");
        player.sendMessage("§e/duel decline §7- Refuser une demande de duel");
        player.sendMessage("§e/duel cancel §7- Annuler vos demandes en attente");
    }

    private boolean ensurePracticeMode(Player player) {
        if (!plugin.getTournamentManager().isPracticeMode()) {
            player.sendMessage("§cLe mode Duel / Pratique est désactivé. Demandez à un administrateur d'activer ce mode.");
            return false;
        }
        return true;
    }
}
