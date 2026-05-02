package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.menu.BracketMenu;
import fr.rammex.chasetag.lobby.menu.MapSelectionMenu;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {
    private final java.util.Map<java.util.UUID, Integer> selectedMatch = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, String> selectedMatchPhase = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> selectedPool = new java.util.HashMap<>();

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.getItemMeta() == null) {
            return;
        }

        if (holder instanceof Menu) {
            event.setCancelled(true);
            if (holder instanceof MapSelectionMenu mapSelectionMenu) {
                mapSelectionMenu.handleClick(event);
                return;
            }
            if (holder instanceof LobbyMenu lobbyMenu) {
                lobbyMenu.handleClick(event);
                return;
            }
            if (holder instanceof PlayerSelectionMenu playerSelectionMenu) {
                playerSelectionMenu.handleClick(event);
                return;
            }
            if (holder instanceof ActiveGamesMenu activeGamesMenu) {
                activeGamesMenu.handleClick(event);
                return;
            }
            String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            TournamentManager tournamentManager = ChaseTagLobby.getInstance().getTournamentManager();

            if (title.equals("Menu Principal") && event.getSlot() == 53) {
                if (player.hasPermission("chasetag.admin.menu")) {
                    new TournamentAdminMenu(player, tournamentManager).open();
                } else {
                    player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'ouvrir le menu administrateur.");
                }
                return;
            }

            if (title.equals("Admin Tournoi")) {
                handleAdminMenuClick(player, currentItem, tournamentManager);
                return;
            }

            if (title.startsWith("Poule ")) {
                handlePouleMenuClick(player, currentItem, tournamentManager, title);
                return;
            }

            if (title.startsWith("Bracket ")) {
                handleBracketMenuClick(player, currentItem, tournamentManager, title.replaceFirst("Bracket ", ""));
                return;
            }

            if (title.equals("Élimination joueurs")) {
                handleEliminationMenuClick(player, currentItem, tournamentManager);
                return;
            }
        } else {
            if (currentItem.getItemMeta().getDisplayName().contains("Menu Principal")) {
                event.setCancelled(true);
            }
        }
    }

    private void handleAdminMenuClick(Player player, ItemStack item, TournamentManager tournamentManager) {
        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.startsWith(ChatColor.GOLD + "Poule ")) {
            String pouleLabel = ChatColor.stripColor(displayName).replace("Poule ", "");
            try {
                int poule = Integer.parseInt(pouleLabel);
                new PouleMenu(player, tournamentManager, poule).open();
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        if (displayName.startsWith(ChatColor.YELLOW + "Mode: ")) {
            boolean practiceMode = tournamentManager.toggleMode();
            player.sendMessage(practiceMode
                    ? ChatColor.GREEN + "Mode Duel / Pratique activé. Les joueurs peuvent maintenant s'envoyer des demandes de duel."
                    : ChatColor.GREEN + "Mode Tournoi activé. Les demandes de duel sont désactivées.");
            new TournamentAdminMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.startsWith(ChatColor.GOLD + "Match ") || displayName.equals(ChatColor.AQUA + "Gérer le bracket")) {
            new BracketMenu(player, tournamentManager, tournamentManager.getCurrentPhase()).open();
            return;
        }

        if (displayName.equals(ChatColor.GREEN + "Passer à la phase suivante")) {
            tournamentManager.nextPhase();
            new TournamentAdminMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.equals(ChatColor.RED + "Réinitialiser la phase")) {
            tournamentManager.setPhase("Phase 1");
            new TournamentAdminMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.equals(ChatColor.RED + "Gérer l'élimination")) {
            new EliminationMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.equals(ChatColor.DARK_RED + "Joueurs éliminés")) {
            new EliminationMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.contains("Lancement des matchs")) {
            boolean started = !tournamentManager.isMatchesStarted();
            tournamentManager.setMatchesStarted(started);
            player.sendMessage(started ? ChatColor.GREEN + "Lancement des matchs autorisé !" : ChatColor.RED + "Lancement des matchs bloqué.");
            new TournamentAdminMenu(player, tournamentManager).open();
            return;
        }

        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new LobbyMenu(player).open();
        }
    }

    private void handlePouleMenuClick(Player player, ItemStack item, TournamentManager tournamentManager, String title) {
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new TournamentAdminMenu(player, tournamentManager).open();
            selectedMatch.remove(player.getUniqueId());
            selectedMatchPhase.remove(player.getUniqueId());
            selectedPool.remove(player.getUniqueId());
            return;
        }

        if (displayName.equals(ChatColor.AQUA + "Choisir la map")) {
            Integer matchId = selectedMatch.get(player.getUniqueId());
            String selectedPhase = selectedMatchPhase.get(player.getUniqueId());
            Integer selectedPoule = selectedPool.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null || selectedPoule == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match.");
                return;
            }
            new MapSelectionMenu(player, ChaseTagLobby.getInstance(), tournamentManager, selectedPhase, selectedPoule, matchId).open();
            return;
        }

        int poule = 0;
        try {
            poule = Integer.parseInt(title.replace("Poule ", ""));
        } catch (NumberFormatException ignored) {
        }
        if (poule == 0) {
            return;
        }

        if (displayName.startsWith(ChatColor.GOLD + "Match ")) {
            String matchLabel = ChatColor.stripColor(displayName).replace("Match ", "");
            try {
                int matchId = Integer.parseInt(matchLabel);
                selectedMatch.put(player.getUniqueId(), matchId);
                selectedMatchPhase.put(player.getUniqueId(), tournamentManager.getCurrentPhase());
                selectedPool.put(player.getUniqueId(), poule);
                player.sendMessage(ChatColor.YELLOW + "Match " + matchId + " de la poule " + poule + " sélectionné. Choisissez un joueur à assigner.");
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        String playerName = ChatColor.stripColor(displayName);
        org.bukkit.entity.Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Joueur introuvable.");
            return;
        }

        Integer selectedMatchId = selectedMatch.get(player.getUniqueId());
        String selectedPhase = selectedMatchPhase.get(player.getUniqueId());
        Integer selectedPoule = selectedPool.get(player.getUniqueId());
        if (selectedMatchId != null && selectedPhase != null && selectedPoule != null) {
            tournamentManager.assignPlayerToMatch(selectedPhase, selectedPoule, selectedMatchId, target.getName());
            player.sendMessage(ChatColor.GREEN + "Joueur " + target.getName() + " assigné au match " + selectedMatchId + " de la poule " + selectedPoule + ".");
            new PouleMenu(player, tournamentManager, poule).open();
            return;
        }

        if (tournamentManager.getPlayerPoule(target.getName()) == poule) {
            tournamentManager.unassignPlayer(target.getName());
            player.sendMessage(ChatColor.YELLOW + "Joueur " + target.getName() + " désassigné de la poule " + poule + ".");
        } else {
            tournamentManager.setPlayerPoule(target.getName(), poule);
            player.sendMessage(ChatColor.GREEN + "Joueur " + target.getName() + " assigné à la poule " + poule + ".");
        }
        new PouleMenu(player, tournamentManager, poule).open();
    }

    private void handleBracketMenuClick(Player player, ItemStack item, TournamentManager tournamentManager, String phase) {
        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new TournamentAdminMenu(player, tournamentManager).open();
            selectedMatch.remove(player.getUniqueId());
            selectedMatchPhase.remove(player.getUniqueId());
            return;
        }

        if (displayName.equals(ChatColor.AQUA + "Choisir la map")) {
            Integer matchId = selectedMatch.get(player.getUniqueId());
            String selectedPhase = selectedMatchPhase.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match.");
                return;
            }
            new MapSelectionMenu(player, ChaseTagLobby.getInstance(), tournamentManager, selectedPhase, 0, matchId).open();
            return;
        }

        if (displayName.startsWith(ChatColor.GOLD + "Match ")) {
            String matchLabel = ChatColor.stripColor(displayName).replace("Match ", "");
            try {
                int matchId = Integer.parseInt(matchLabel);
                selectedMatch.put(player.getUniqueId(), matchId);
                selectedMatchPhase.put(player.getUniqueId(), phase);
                player.sendMessage(ChatColor.YELLOW + "Match " + matchId + " sélectionné. Choisissez un joueur à assigner.");
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        if (displayName.startsWith(ChatColor.GREEN.toString()) || displayName.startsWith(ChatColor.AQUA.toString())) {
            Integer matchId = selectedMatch.get(player.getUniqueId());
            String selectedPhase = selectedMatchPhase.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match à configurer.");
                return;
            }

            String playerName = ChatColor.stripColor(displayName);
            org.bukkit.entity.Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Joueur introuvable.");
                return;
            }

            tournamentManager.assignPlayerToMatch(selectedPhase, matchId, target.getName());
            player.sendMessage(ChatColor.GREEN + "Joueur " + target.getName() + " assigné au match " + matchId + " de " + selectedPhase + ".");
            new BracketMenu(player, tournamentManager, phase).open();
            return;
        }
    }

    private void handleEliminationMenuClick(Player player, ItemStack item, TournamentManager tournamentManager) {
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new TournamentAdminMenu(player, tournamentManager).open();
            return;
        }

        String playerName = ChatColor.stripColor(displayName);
        org.bukkit.entity.Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Joueur introuvable.");
            return;
        }

        boolean eliminated = tournamentManager.isPlayerEliminated(target.getName());
        tournamentManager.setPlayerEliminated(target.getName(), !eliminated);
        player.sendMessage(eliminated
                ? ChatColor.GREEN + "Joueur " + target.getName() + " restauré."
                : ChatColor.RED + "Joueur " + target.getName() + " éliminé.");
        new EliminationMenu(player, tournamentManager).open();
    }
}
