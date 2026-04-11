package fr.rammex.chasetag.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import fr.rammex.chasetag.velocity.ChaseTagVelocity;

public class PlayerConnectionListener {

    private final ChaseTagVelocity plugin;

    public PlayerConnectionListener(ChaseTagVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        // Envoyer tout nouveau joueur vers le lobby par défaut
        String lobbyName = plugin.getConfig().getLobbyServerName();
        plugin.getProxy().getServer(lobbyName)
            .ifPresent(event::setInitialServer);
    }
}