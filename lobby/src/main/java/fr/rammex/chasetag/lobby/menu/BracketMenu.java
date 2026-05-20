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

public class BracketMenu extends Menu {
    private final TournamentManager tournamentManager;
    private final Inventory inventory;
    private final String phaseName;

    public BracketMenu(Player player, TournamentManager tournamentManager, String phaseName) {
        super(player);
        this.tournamentManager = tournamentManager;
        this.inventory = Bukkit.createInventory(this, 54, "Bracket " + phaseName);
        this.phaseName = phaseName;
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

        List<TournamentMatch> matches = tournamentManager.getMatchesForPhase(phaseName);
        int matchSlot = 10;
        Integer selectedMatchId = MenuListener.selectedMatch.get(player.getUniqueId());
        String selectedPhase = MenuListener.selectedMatchPhase.get(player.getUniqueId());

        for (TournamentMatch match : matches) {
            String player1Name = match.getPlayer1();
            String player2Name = match.getPlayer2();
            org.bukkit.entity.Player onlinePlayer1 = player1Name == null ? null : Bukkit.getPlayerExact(player1Name);
            org.bukkit.entity.Player onlinePlayer2 = player2Name == null ? null : Bukkit.getPlayerExact(player2Name);
            String player1 = onlinePlayer1 != null ? onlinePlayer1.getName() : match.getPlayer1String();
            String player2 = onlinePlayer2 != null ? onlinePlayer2.getName() : match.getPlayer2String();
            List<String> matchLore = new ArrayList<>();
            matchLore.add(ChatColor.GRAY + player1 + " vs " + player2);
            matchLore.add(ChatColor.GRAY + "Map: " + (match.hasMap() ? match.getMapName() : "Aucune"));
            
            boolean isSelected = selectedMatchId != null && selectedMatchId == match.getMatchId() && phaseName.equals(selectedPhase);
            
            if (match.hasMap() && match.getPlayer1() != null && match.getPlayer2() != null) {
                matchLore.add(ChatColor.GREEN + "Prêt à démarrer");
            } else {
                matchLore.add(ChatColor.GRAY + "Clic pour reconfigurer");
            }
            
            if (isSelected) {
                matchLore.add("");
                matchLore.add(ChatColor.YELLOW + ">>> MATCH SÉLECTIONNÉ <<<");
            }

            ItemStack matchItem = MenuUtils.createMenuItem(
                    isSelected ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE,
                    ChatColor.GOLD + "Match " + match.getMatchId(),
                    matchLore
            );
            if (matchSlot < 53) {
                inventory.setItem(matchSlot, matchItem);
            }
            matchSlot += 2;
        }

        int playerSlot = 28;
        for (String playerName : tournamentManager.getRemainingPlayers()) {
            if (playerSlot >= 52) {
                break;
            }
            org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayerExact(playerName);
            if (onlinePlayer == null) {
                continue;
            }
            inventory.setItem(playerSlot, MenuUtils.createPlayerHead(
                    onlinePlayer,
                    ChatColor.GREEN + onlinePlayer.getName(),
                    List.of(ChatColor.GRAY + "Clic pour sélectionner ce joueur")
            ));
            playerSlot++;
        }

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
            return;
        }

        if (displayName.equals(ChatColor.AQUA + "Choisir la map")) {
            Integer matchId = MenuListener.selectedMatch.get(player.getUniqueId());
            String selectedPhase = MenuListener.selectedMatchPhase.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match.");
                return;
            }
            new MapSelectionMenu(player, fr.rammex.chasetag.lobby.ChaseTagLobby.getInstance(), tournamentManager, selectedPhase, 0, matchId).open();
            return;
        }

        if (displayName.startsWith(ChatColor.GOLD + "Match ")) {
            String matchLabel = ChatColor.stripColor(displayName).replace("Match ", "");
            try {
                int matchId = Integer.parseInt(matchLabel);
                MenuListener.selectedMatch.put(player.getUniqueId(), matchId);
                MenuListener.selectedMatchPhase.put(player.getUniqueId(), phaseName);
                player.sendMessage(ChatColor.YELLOW + "Match " + matchId + " sélectionné. Choisissez un joueur à assigner.");
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        if (displayName.startsWith(ChatColor.GREEN.toString()) || displayName.startsWith(ChatColor.AQUA.toString())) {
            Integer matchId = MenuListener.selectedMatch.get(player.getUniqueId());
            String selectedPhase = MenuListener.selectedMatchPhase.get(player.getUniqueId());
            if (matchId == null || selectedPhase == null) {
                player.sendMessage(ChatColor.RED + "Sélectionnez d'abord un match à configurer.");
                return;
            }

            String playerName = ChatColor.stripColor(displayName);
            tournamentManager.assignPlayerToMatch(selectedPhase, matchId, playerName);
            player.sendMessage(ChatColor.GREEN + "Joueur " + playerName + " assigné au match " + matchId + " de " + selectedPhase + ".");
            
            // On réinitialise la sélection
            MenuListener.selectedMatch.remove(player.getUniqueId());
            MenuListener.selectedMatchPhase.remove(player.getUniqueId());
            
            open(); // Refresh
            return;
        }
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }

    public String getPhase() {
        return phaseName;
    }
}
