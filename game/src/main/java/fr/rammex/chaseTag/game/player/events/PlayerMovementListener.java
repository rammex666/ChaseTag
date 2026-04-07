package fr.rammex.chaseTag.game.player.events;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.game.Game;
import fr.rammex.chaseTag.game.game.GameState;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMovementListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game == null || game.getGameState() != GameState.PLAYING) return;

        // Bloquer le mouvement pendant le countdown (permet quand même de tourner la tête)
        if (game.isCountdown()) {
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getY() != event.getTo().getY() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
            return;
        }

        // On ne vérifie que si le joueur a changé de bloc pour économiser des ressources
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (!game.getArena().isInside(event.getTo())) {
            // Empêcher la sortie
            event.setCancelled(true);
            // On le téléporte légèrement vers l'intérieur pour éviter les tremblements
            event.getPlayer().teleport(event.getFrom());
            event.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas sortir de l'arène !");
        }
    }
}
