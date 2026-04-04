package fr.rammex.chaseTag.game.player.events;

import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.Role;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoinFirstTime(PlayerJoinEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        if(!player.hasPlayedBefore()){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Role.None);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
        }


        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        if(player1 == null){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Role.None);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
            player1 = PlayerManager.getPlayer(player.getUniqueId().toString());
        }
    }

    @EventHandler
    public void onPlayerMessage(AsyncChatEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        Role playerRole = player1.getPlayerRole();

        String newMessage = playerRole.getPrefix()+" "+player.getName()+" >> "+message;
        Bukkit.broadcast(Component.text(newMessage));
    }
}
