package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.tournament.TournamentMatch;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PouleMenu extends Menu {
    private final TournamentManager tournamentManager;
    private final int poule;

    public PouleMenu(Player player, TournamentManager tournamentManager, int poule) {
        super(player);
        this.tournamentManager = tournamentManager;
        this.poule = poule;
        this.inventory = Bukkit.createInventory(this, 54, "Poule " + tournamentManager.getPoolLetter(poule));
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        ItemStack background = MenuUtils.createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        List<String> poolPlayers = tournamentManager.getPlayersInPoule(poule);
        List<TournamentMatch> matches = tournamentManager.getMatchesForPool(tournamentManager.getCurrentPhase(), poule);

        int slot = 10;
        for (String playerName : poolPlayers) {
            if (slot >= 26) {
                break;
            }
            org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayerExact(playerName);
            boolean eliminated = tournamentManager.isPlayerEliminated(playerName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Poule: " + tournamentManager.getPoolLetter(poule));
            lore.add(ChatColor.GRAY + "Éliminé: " + (eliminated ? "Oui" : "Non"));
            lore.add(ChatColor.GRAY + "Clic pour désassigner de cette poule");
            ItemStack head = onlinePlayer != null
                    ? MenuUtils.createPlayerHead(onlinePlayer, ChatColor.GREEN + playerName, lore)
                    : MenuUtils.createMenuItem(Material.PLAYER_HEAD, ChatColor.GREEN + playerName, lore);
            inventory.setItem(slot, head);
            slot++;
        }

        int matchSlot = 28;
        for (TournamentMatch match : matches) {
            String player1Name = match.getPlayer1String();
            String player2Name = match.getPlayer2String();
            org.bukkit.entity.Player onlinePlayer1 = Bukkit.getPlayerExact(player1Name);
            org.bukkit.entity.Player onlinePlayer2 = Bukkit.getPlayerExact(player2Name);
            String player1 = onlinePlayer1 != null ? onlinePlayer1.getName() : player1Name;
            String player2 = onlinePlayer2 != null ? onlinePlayer2.getName() : player2Name;
            List<String> matchLore = new ArrayList<>();
            matchLore.add(ChatColor.GRAY + player1 + " vs " + player2);
            matchLore.add(ChatColor.GRAY + "Map: " + (match.hasMap() ? match.getMapName() : "Aucune"));
            if (match.hasMap() && match.getPlayer1() != null && match.getPlayer2() != null) {
                matchLore.add(ChatColor.GREEN + "Prêt à démarrer");
            } else {
                matchLore.add(ChatColor.GRAY + "Clic pour reconfigurer");
            }
            ItemStack matchItem = MenuUtils.createMenuItem(
                    Material.YELLOW_CONCRETE,
                    ChatColor.GOLD + "Match " + match.getMatchId(),
                    matchLore
            );
            if (matchSlot < 53) {
                inventory.setItem(matchSlot, matchItem);
            }
            matchSlot += 2;
        }

        ItemStack assignToPool = MenuUtils.createMenuItem(
                Material.NAME_TAG,
                ChatColor.GREEN + "Assigner un joueur à la poule",
                List.of(ChatColor.GRAY + "Clique ici puis écris le pseudo", ChatColor.GRAY + "du joueur dans le chat.")
        );
        inventory.setItem(48, assignToPool);

        ItemStack chooseMap = MenuUtils.createMenuItem(
                Material.MAP,
                ChatColor.AQUA + "Choisir la map",
                List.of(ChatColor.GRAY + "Sélectionne un match puis clique ici.")
        );
        inventory.setItem(49, chooseMap);

        ItemStack back = MenuUtils.createBackButton(ChatColor.YELLOW + "Retour", ChatColor.GRAY + "Retour au menu administrateur");
        inventory.setItem(53, back);
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            new TournamentAdminMenu(player, tournamentManager).open();
            MenuListener.selectedMatch.remove(player.getUniqueId());
            MenuListener.selectedMatchPhase.remove(player.getUniqueId());
            MenuListener.selectedPool.remove(player.getUniqueId());
            return;
        }

        if (displayName.equals(ChatColor.AQUA + "Choisir la map")) {
            Integer matchId = MenuListener.selectedMatch.get(player.getUniqueId());
            String selectedPhase = MenuListener.selectedMatchPhase.get(player.getUniqueId());
            Integer selectedPoule = MenuListener.selectedPool.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null || selectedPoule == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match.");
                return;
            }
            new MapSelectionMenu(player, fr.rammex.chasetag.lobby.ChaseTagLobby.getInstance(), tournamentManager, selectedPhase, selectedPoule, matchId).open();
            return;
        }

        if (displayName.equals(ChatColor.GREEN + "Assigner un joueur à la poule")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Écris le pseudo du joueur à assigner à la poule " + tournamentManager.getPoolLetter(poule) + " dans le chat.");
            player.sendMessage(ChatColor.GRAY + "(Ou écris 'cancel' pour annuler)");
            MenuListener.waitingForChatInput.put(player.getUniqueId(), "POOL_ASSIGN:" + poule);
            return;
        }

        if (displayName.startsWith(ChatColor.GOLD + "Match ")) {
            String matchLabel = ChatColor.stripColor(displayName).replace("Match ", "");
            try {
                int matchId = Integer.parseInt(matchLabel);
                MenuListener.selectedMatch.put(player.getUniqueId(), matchId);
                MenuListener.selectedMatchPhase.put(player.getUniqueId(), tournamentManager.getCurrentPhase());
                MenuListener.selectedPool.put(player.getUniqueId(), poule);
                player.sendMessage(ChatColor.YELLOW + "Match " + matchId + " de la poule " + tournamentManager.getPoolLetter(poule) + " sélectionné.");
                player.sendMessage(ChatColor.YELLOW + "Maintenant, clique sur 'Assigner un joueur à la poule' pour choisir le joueur.");
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        String playerName = ChatColor.stripColor(displayName);

        // Si un match est sélectionné, on assigne au match
        Integer selectedMatchId = MenuListener.selectedMatch.get(player.getUniqueId());
        String selectedPhase = MenuListener.selectedMatchPhase.get(player.getUniqueId());
        Integer selectedPoule = MenuListener.selectedPool.get(player.getUniqueId());

        if (selectedMatchId != null && selectedPhase != null && selectedPoule != null && selectedPoule == poule) {
            // On vérifie si le joueur est dans la poule ou en ligne
            if (tournamentManager.getPlayersInPoule(poule).contains(playerName) || Bukkit.getPlayerExact(playerName) != null) {
                tournamentManager.assignPlayerToMatch(selectedPhase, selectedPoule, selectedMatchId, playerName);
                player.sendMessage(ChatColor.GREEN + "Joueur " + playerName + " assigné au match " + selectedMatchId + " de la poule " + tournamentManager.getPoolLetter(selectedPoule) + ".");
                
                // On réinitialise la sélection après l'assignation
                MenuListener.selectedMatch.remove(player.getUniqueId());
                MenuListener.selectedMatchPhase.remove(player.getUniqueId());
                MenuListener.selectedPool.remove(player.getUniqueId());
                
                open(); // Refresh
                return;
            }
        }

        // Sinon, gestion de l'assignation à la poule (Désassignation ici car on clique sur une tête déjà présente)
        if (tournamentManager.getPlayersInPoule(poule).contains(playerName)) {
            tournamentManager.unassignPlayer(playerName);
            player.sendMessage(ChatColor.YELLOW + "Joueur " + playerName + " désassigné de la poule " + tournamentManager.getPoolLetter(poule) + ".");
            open(); // Refresh
        }
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }
}
