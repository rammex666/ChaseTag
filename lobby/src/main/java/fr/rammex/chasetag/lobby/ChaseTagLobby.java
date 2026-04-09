package fr.rammex.chasetag.lobby;

import fr.rammex.chasetag.lobby.command.PlayCommand;
import fr.rammex.chasetag.lobby.database.MongoManager;
import fr.rammex.chasetag.lobby.game.GameManager;
import fr.rammex.chasetag.lobby.listener.LobbyListener;
import fr.rammex.chasetag.lobby.menu.MenuListener;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.events.PlayerLobbyEvents;
import fr.rammex.chasetag.lobby.pterodactyl.PterodactylClient;
import fr.rammex.chasetag.lobby.redis.LobbyRedisListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class ChaseTagLobby extends JavaPlugin {

    private static ChaseTagLobby instance;
    private JedisPool jedisPool;
    private MongoManager mongoManager;
    private PterodactylClient pterodactylClient;
    private GameManager gameManager;
    private LobbyRedisListener redisListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // MongoDB
        this.mongoManager = new MongoManager(
            getConfig().getString("mongodb.uri", "mongodb://localhost:27017"),
            getConfig().getString("mongodb.database", "chasetag")
        );

        // Redis
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        
        String redisHost = getConfig().getString("redis.host", "localhost");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            this.jedisPool = new JedisPool(
                poolConfig,
                redisHost,
                redisPort,
                2000, // timeout
                redisPassword
            );
        } else {
            this.jedisPool = new JedisPool(
                poolConfig,
                redisHost,
                redisPort
            );
        }

        // Pterodactyl
        this.pterodactylClient = new PterodactylClient(
            getConfig().getString("pterodactyl.url"),
            getConfig().getString("pterodactyl.api-key"),
            getConfig().getInt("pterodactyl.egg-id")
        );

        // GameManager
        this.gameManager = new GameManager(this);

        // Listener Redis (thread séparé)
        this.redisListener = new LobbyRedisListener(this);
        this.redisListener.start();

        PlayerManager.init(this.getDataFolder());

        // Commandes
        getCommand("chasetag").setExecutor(new PlayCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLobbyEvents(), this);

        getLogger().info("ChaseTagLobby activé.");
    }

    @Override
    public void onDisable() {
        if (redisListener != null) redisListener.stop();
        if (jedisPool != null) jedisPool.close();
        if (mongoManager != null) mongoManager.close();
        PlayerManager.save();
        getLogger().info("ChaseTagLobby désactivé.");
    }

    public static ChaseTagLobby getInstance() { return instance; }
    public JedisPool getJedisPool() { return jedisPool; }
    public MongoManager getMongoManager() { return mongoManager; }
    public PterodactylClient getPterodactylClient() { return pterodactylClient; }
    public GameManager getGameManager() { return gameManager; }
}