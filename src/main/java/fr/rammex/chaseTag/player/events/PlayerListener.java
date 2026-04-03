package fr.rammex.chaseTag.player.events;

import fr.rammex.chaseTag.player.PlayerManager;
import fr.rammex.chaseTag.player.Rank;
import fr.rammex.chaseTag.player.Role;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoinFirstTime(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if(!player.hasPlayedBefore()){
            fr.rammex.chaseTag.player.Player playerRegistry = new fr.rammex.chaseTag.player.Player(player.getUniqueId().toString(), Role.None, Rank.Player);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
        }


        fr.rammex.chaseTag.player.Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        if(player1 == null){
            fr.rammex.chaseTag.player.Player playerRegistry = new fr.rammex.chaseTag.player.Player(player.getUniqueId().toString(), Role.None, Rank.Player);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
            player1 = PlayerManager.getPlayer(player.getUniqueId().toString());
        }

        event.setJoinMessage(player1.getPlayerRank().getPrefix()+" "+player.getName()+" à rejoint le serveur.");
    }

    @EventHandler
    public void onPlayerMessage(AsyncChatEvent event){
        Player player = event.getPlayer();
        fr.rammex.chaseTag.player.Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        Role playerRole = player1.getPlayerRole();
        Rank playerRank = player1.getPlayerRank();

        String newMessage = playerRank.getPrefix()+" "+playerRole.getPrefix()+" "+player.getName()+" >> "+message;
        Bukkit.broadcast(Component.text(newMessage));
    }
}
