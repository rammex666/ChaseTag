package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EliminationMenu extends Menu {
    private final TournamentManager tournamentManager;
    private final Inventory inventory;

    public EliminationMenu(Player player, TournamentManager tournamentManager) {
        super(player);
        this.tournamentManager = tournamentManager;
        this.inventory = Bukkit.createInventory(this, 54, "Élimination joueurs");
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

        int slot = 10;
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= 53) {
                break;
            }
            String playerName = onlinePlayer.getName();
            boolean eliminated = tournamentManager.isPlayerEliminated(playerName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Poule: " + tournamentManager.getPouleLabel(playerName));
            lore.add(ChatColor.GRAY + "Éliminé: " + (eliminated ? "Oui" : "Non"));
            lore.add(" ");
            lore.add(eliminated ? ChatColor.RED + "Clic pour restaurer" : ChatColor.GREEN + "Clic pour éliminer");
            inventory.setItem(slot, MenuUtils.createPlayerHead(onlinePlayer, ChatColor.GREEN + playerName, lore));
            slot++;
        }

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
            return;
        }

        String playerName = ChatColor.stripColor(displayName);
        // On vérifie en ligne ou en base
        if (Bukkit.getPlayerExact(playerName) != null || fr.rammex.chasetag.lobby.ChaseTagLobby.getInstance().getPlayerMongoRepository().getPlayerByName(playerName).isPresent()) {
            boolean eliminated = tournamentManager.isPlayerEliminated(playerName);
            tournamentManager.setPlayerEliminated(playerName, !eliminated);
            player.sendMessage(eliminated
                    ? ChatColor.GREEN + "Joueur " + playerName + " restauré."
                    : ChatColor.RED + "Joueur " + playerName + " éliminé.");
            open(); // Refresh
        }
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }
}
