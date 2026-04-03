package fr.rammex.chaseTag.game;

import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.events.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChaseTag extends JavaPlugin {
    private static ChaseTag instance;
    private PlayerManager playerManager;


    @Override
    public void onEnable() {
        instance = this;

        this.playerManager = new PlayerManager();
        PlayerManager.init(this.getDataFolder());


        loadEvents();

    }

    @Override
    public void onDisable() {
        PlayerManager.save();
    }

    public static ChaseTag getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private void loadEvents(){
        this.getServer().getPluginManager().registerEvents(new PlayerListener(),this);
    }
}
