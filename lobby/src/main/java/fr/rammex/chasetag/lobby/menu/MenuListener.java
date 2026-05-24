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

import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.EventPriority;

public class MenuListener implements Listener {
    public static final java.util.Map<java.util.UUID, Integer> selectedMatch = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, String> selectedMatchPhase = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, Integer> selectedPool = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, String> waitingForChatInput = new java.util.HashMap<>();

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.getItemMeta() == null) {
            return;
        }

        if (holder instanceof Menu menu) {
            event.setCancelled(true);
            menu.handleClick(event);
            return;
        }
 else {
            if (currentItem.getItemMeta().getDisplayName().contains("Menu Principal")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String context = waitingForChatInput.get(player.getUniqueId());
        
        if (context == null) return;
        
        event.setCancelled(true);
        String playerName = event.getMessage().trim();
        waitingForChatInput.remove(player.getUniqueId());

        if (playerName.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Action annulée.");
            return;
        }

        TournamentManager tm = ChaseTagLobby.getInstance().getTournamentManager();

        if (context.startsWith("POOL_ASSIGN:")) {
            int poule = Integer.parseInt(context.split(":")[1]);
            
            // On vérifie en DB si le joueur existe
            ChaseTagLobby.getInstance().getPlayerMongoRepository().getPlayerByName(playerName).ifPresentOrElse(dbPlayer -> {
                String finalName = dbPlayer.getPlayerName();
                
                // Si un match est sélectionné, on assigne au match
                Integer matchId = selectedMatch.get(player.getUniqueId());
                String phase = selectedMatchPhase.get(player.getUniqueId());
                Integer poolForMatch = selectedPool.get(player.getUniqueId());

                if (matchId != null && phase != null && poolForMatch != null && poolForMatch == poule) {
                    tm.assignPlayerToMatch(phase, poule, matchId, finalName);
                    player.sendMessage(ChatColor.GREEN + "Joueur " + finalName + " assigné au match " + matchId + " de la poule " + tm.getPoolLetter(poule) + ".");
                    selectedMatch.remove(player.getUniqueId());
                    selectedMatchPhase.remove(player.getUniqueId());
                    selectedPool.remove(player.getUniqueId());
                } else {
                    // Sinon assignation à la poule simple
                    tm.setPlayerPoule(finalName, poule);
                    player.sendMessage(ChatColor.GREEN + "Joueur " + finalName + " assigné à la poule " + tm.getPoolLetter(poule) + ".");
                }

                // Réouvrir le menu sur le thread principal
                Bukkit.getScheduler().runTask(ChaseTagLobby.getInstance(), () -> {
                    new PouleMenu(player, tm, poule).open();
                });
            }, () -> {
                player.sendMessage(ChatColor.RED + "Joueur '" + playerName + "' introuvable dans la base de données (non WL).");
                Bukkit.getScheduler().runTask(ChaseTagLobby.getInstance(), () -> {
                    new PouleMenu(player, tm, poule).open();
                });
            });
        }
    }
}
