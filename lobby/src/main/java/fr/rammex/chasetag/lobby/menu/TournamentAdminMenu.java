package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.tournament.PhaseConfig;
import fr.rammex.chasetag.lobby.tournament.PhaseType;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TournamentAdminMenu extends Menu {
    private final TournamentManager tournamentManager;
    private final Inventory inventory;

    public TournamentAdminMenu(Player player, TournamentManager tournamentManager) {
        super(player);
        this.tournamentManager = tournamentManager;
        this.inventory = Bukkit.createInventory(this, 54, "Admin Tournoi");
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        ItemStack glass = MenuUtils.createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }
              
        PhaseConfig currentPhase = tournamentManager.getCurrentPhaseConfig().orElse(new PhaseConfig(tournamentManager.getCurrentPhase(), PhaseType.POULE, 8, 4, 0));
        boolean poolPhase = currentPhase.isPoule();

        if (poolPhase) {
            for (int i = 1; i <= currentPhase.getPouleCount(); i++) {
                ItemStack pouleItem = MenuUtils.createMenuItem(
                        Material.ORANGE_CONCRETE,
                        ChatColor.GOLD + "Poule " + i,
                        List.of(
                                ChatColor.GRAY + "Joueurs: " + tournamentManager.getPlayersCountInPoule(i) + "/" + currentPhase.getPlayersPerPoule(),
                                ChatColor.GRAY + "Clic pour ouvrir la poule",
                                ChatColor.DARK_GRAY + "Phase actuelle: " + tournamentManager.getCurrentPhase())
                );
                inventory.setItem(8 + i, pouleItem);
            }
        } else {
            int bracketSize = currentPhase.getBracketSize();
            int matchCount = Math.max(1, bracketSize / 2);
            for (int i = 1; i <= matchCount; i++) {
                ItemStack matchItem = MenuUtils.createMenuItem(
                        Material.RED_CONCRETE,
                        ChatColor.GOLD + "Match " + i,
                        List.of(
                                ChatColor.GRAY + "Clic pour gérer le match",
                                ChatColor.DARK_GRAY + "Phase actuelle: " + tournamentManager.getCurrentPhase()
                        )
                );
                inventory.setItem(8 + i, matchItem);
            }

            ItemStack bracketInfo = MenuUtils.createMenuItem(
                    Material.PAPER,
                    ChatColor.AQUA + "Gérer le bracket",
                    List.of(
                            ChatColor.GRAY + "Phase: " + tournamentManager.getCurrentPhase(),
                            ChatColor.GRAY + "Joueurs restants: " + tournamentManager.getRemainingPlayers().size()
                    )
            );
            inventory.setItem(18, bracketInfo);
        }

        String nextPhaseLabel = tournamentManager.getCurrentPhase().matches("Phase \\d+")
                ? "Phase " + (Integer.parseInt(tournamentManager.getCurrentPhase().replaceAll("\\D", "")) + 1)
                : "Phase 2";

        ItemStack phaseInfo = MenuUtils.createMenuItem(
                Material.PAPER,
                ChatColor.AQUA + "Phase actuelle",
                List.of(
                        ChatColor.GRAY + tournamentManager.getCurrentPhase(),
                        " ",
                        ChatColor.YELLOW + "Phase suivante: " + ChatColor.WHITE + nextPhaseLabel,
                        ChatColor.GRAY + "Clique gauche pour passer à la phase suivante"
                )
        );
        inventory.setItem(20, phaseInfo);

        ItemStack modeItem = MenuUtils.createMenuItem(
                Material.ENDER_EYE,
                ChatColor.YELLOW + "Mode: " + (tournamentManager.isPracticeMode() ? "Duel / Pratique" : "Tournoi"),
                List.of(
                        ChatColor.GRAY + "Clique pour basculer en mode " + (tournamentManager.isPracticeMode() ? "Tournoi" : "Duel"),
                        ChatColor.GRAY + "Mode actuel: " + (tournamentManager.isPracticeMode() ? "Pratique" : "Tournoi")
                )
        );
        inventory.setItem(21, modeItem);

        ItemStack nextPhase = MenuUtils.createMenuItem(
                Material.CLOCK,
                ChatColor.GREEN + "Passer à la phase suivante",
                List.of(ChatColor.GRAY + "Clique pour avancer la phase")
        );
        inventory.setItem(22, nextPhase);

        ItemStack resetPhase = MenuUtils.createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Réinitialiser la phase",
                List.of(ChatColor.GRAY + "Remet la phase à Phase 1")
        );
        inventory.setItem(24, resetPhase);

        ItemStack elimination = MenuUtils.createMenuItem(
                Material.REDSTONE,
                ChatColor.RED + "Gérer l'élimination",
                List.of(ChatColor.GRAY + "Clic pour ouvrir les joueurs à éliminer")
        );
        inventory.setItem(30, elimination);

        ItemStack eliminatedList = MenuUtils.createMenuItem(
                Material.BARRIER,
                ChatColor.DARK_RED + "Joueurs éliminés",
                List.of(
                        ChatColor.GRAY + "Total: " + tournamentManager.getEliminatedPlayers().size(),
                        ChatColor.GRAY + "Clic pour voir / restaurer"
                )
        );
        inventory.setItem(32, eliminatedList);

        ItemStack launchMatches = MenuUtils.createMenuItem(
                tournamentManager.isMatchesStarted() ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (tournamentManager.isMatchesStarted() ? ChatColor.GREEN : ChatColor.RED) + "Lancement des matchs",
                List.of(
                        ChatColor.GRAY + "Statut: " + (tournamentManager.isMatchesStarted() ? ChatColor.GREEN + "AUTORISÉ" : ChatColor.RED + "BLOQUÉ"),
                        ChatColor.GRAY + "Clique pour " + (tournamentManager.isMatchesStarted() ? "bloquer" : "autoriser") + " le lancement des matchs"
                )
        );
        inventory.setItem(31, launchMatches);

        ItemStack back = MenuUtils.createBackButton(ChatColor.YELLOW + "Retour", ChatColor.GRAY + "Retour au menu principal");
        inventory.setItem(53, back);
    }

    public int getPouleFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return 0;
        }
        String displayName = item.getItemMeta().getDisplayName();
        if (!displayName.startsWith(ChatColor.GOLD + "Poule ")) {
            return 0;
        }
        try {
            return Integer.parseInt(displayName.replace(ChatColor.GOLD + "Poule ", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
