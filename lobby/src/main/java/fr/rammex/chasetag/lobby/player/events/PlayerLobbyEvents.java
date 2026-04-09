package fr.rammex.chasetag.lobby.player.events;

import fr.rammex.chasetag.lobby.player.Player;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.rank.Rank;
import fr.rammex.chasetag.lobby.utils.ColorUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLobbyEvents implements Listener {

    @EventHandler
    public void onPlayerMessage(AsyncChatEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        Rank playerRole = player1.getPlayerRole();
        Component prefix;
        if(playerRole.getHex3() == ""){
            prefix = ColorUtils.gradient(playerRole.getPrefix()+" ", playerRole.getHex1(), playerRole.getHex2());
        } else {
            prefix = ColorUtils.gradient(playerRole.getPrefix()+" ", playerRole.getHex1(), playerRole.getHex2(), playerRole.getHex3());
        }

        Component messageColorized = ColorUtils.colorize("<#C2C2C2>"+player.getName()+" >> "+ message);

        Component fullMessage = Component.empty()
                .append(prefix)
                .append(messageColorized);

        Bukkit.broadcast(fullMessage);
    }

    @EventHandler
    public void onPlayerJoinFirstTime(PlayerJoinEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        if(!player.hasPlayedBefore()){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Rank.Joueur);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
        }


        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        if(player1 == null){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Rank.Joueur);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
        }
    }
}
