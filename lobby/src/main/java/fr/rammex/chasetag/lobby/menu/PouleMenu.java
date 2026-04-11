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
        this.inventory = Bukkit.createInventory(this, 54, "Poule " + poule);
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
            lore.add(ChatColor.GRAY + "Poule: " + poule);
            lore.add(ChatColor.GRAY + "Éliminé: " + (eliminated ? "Oui" : "Non"));
            lore.add(ChatColor.GRAY + "Clic pour sélectionner ce joueur");
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

        int assignSlot = 35;
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (assignSlot >= 52) {
                break;
            }
            String onlineName = onlinePlayer.getName();
            if (poolPlayers.contains(onlineName) || tournamentManager.isPlayerEliminated(onlineName)) {
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clic pour assigner à la poule " + poule);
            lore.add(ChatColor.GRAY + "Actuelle: " + tournamentManager.getPouleLabel(onlineName));
            inventory.setItem(assignSlot, MenuUtils.createPlayerHead(onlinePlayer, ChatColor.AQUA + onlineName, lore));
            assignSlot++;
        }

        ItemStack back = MenuUtils.createBackButton(ChatColor.YELLOW + "Retour", ChatColor.GRAY + "Retour au menu administrateur");
        inventory.setItem(53, back);
    }
}
