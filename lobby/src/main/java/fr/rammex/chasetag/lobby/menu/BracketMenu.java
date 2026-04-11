package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.tournament.TournamentMatch;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
        for (TournamentMatch match : matches) {
            String player1Name = match.getPlayer1();
            String player2Name = match.getPlayer2();
            org.bukkit.entity.Player onlinePlayer1 = player1Name == null ? null : Bukkit.getPlayerExact(player1Name);
            org.bukkit.entity.Player onlinePlayer2 = player2Name == null ? null : Bukkit.getPlayerExact(player2Name);
            String player1 = onlinePlayer1 != null ? onlinePlayer1.getName() : match.getPlayer1String();
            String player2 = onlinePlayer2 != null ? onlinePlayer2.getName() : match.getPlayer2String();
            ItemStack matchItem = MenuUtils.createMenuItem(
                    Material.YELLOW_CONCRETE,
                    ChatColor.GOLD + "Match " + match.getMatchId(),
                    List.of(
                            ChatColor.GRAY + player1 + " vs " + player2,
                            ChatColor.GRAY + "Clic pour reconfigurer"
                    )
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

        ItemStack back = MenuUtils.createBackButton(ChatColor.YELLOW + "Retour", ChatColor.GRAY + "Retour au menu administrateur");
        inventory.setItem(53, back);
    }
}
