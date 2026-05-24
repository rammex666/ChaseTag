package fr.rammex.chasetag.lobby;

import fr.rammex.chasetag.lobby.command.DuelCommand;
import fr.rammex.chasetag.lobby.command.PlayCommand;
import fr.rammex.chasetag.lobby.database.MongoManager;
import fr.rammex.chasetag.lobby.discord.DiscordBot;
import fr.rammex.chasetag.lobby.duel.DuelRequestManager;
import fr.rammex.chasetag.lobby.game.GameManager;
import fr.rammex.chasetag.lobby.listener.LobbyListener;
import fr.rammex.chasetag.lobby.menu.MenuListener;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.PlayerMongoRepository;
import fr.rammex.chasetag.lobby.player.events.PlayerLobbyEvents;
import fr.rammex.chasetag.lobby.placeholder.LobbyPlaceholderExpansion;
import fr.rammex.chasetag.lobby.pterodactyl.PterodactylClient;
import fr.rammex.chasetag.lobby.redis.LobbyRedisListener;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import fr.rammex.chasetag.lobby.podium.PodiumManager;
import fr.rammex.chasetag.lobby.podium.PodiumCommand;
import fr.rammex.chasetag.lobby.podium.PodiumListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class ChaseTagLobby extends JavaPlugin {

    private static ChaseTagLobby instance;
    private JedisPool jedisPool;
    private MongoManager mongoManager;
    private PlayerMongoRepository playerMongoRepository;
    private PterodactylClient pterodactylClient;
    private GameManager gameManager;
    private TournamentManager tournamentManager;
    private DuelRequestManager duelRequestManager;
    private LobbyRedisListener redisListener;
    private DiscordBot discordBot;
    private fr.rammex.chasetag.lobby.staff.StaffManager staffManager;
    private fr.rammex.chasetag.lobby.staff.StaffModeManager staffModeManager;
    private PodiumManager podiumManager;

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

        // Repository and managers
        this.playerMongoRepository = new PlayerMongoRepository(this.mongoManager);
        this.tournamentManager = new TournamentManager(this, this.mongoManager);
        this.duelRequestManager = new DuelRequestManager();
        this.staffManager = new fr.rammex.chasetag.lobby.staff.StaffManager(this);
        this.staffModeManager = new fr.rammex.chasetag.lobby.staff.StaffModeManager(this);
        this.podiumManager = new PodiumManager(this);

        // Discord Bot
        this.discordBot = new DiscordBot(this);

        // Listener Redis (thread séparé)
        this.redisListener = new LobbyRedisListener(this);
        this.redisListener.start();

        PlayerManager.init(this.getDataFolder());

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LobbyPlaceholderExpansion(this).register();
        }

        // Commandes
        getCommand("chasetag").setExecutor(new PlayCommand(this));
        getCommand("duel").setExecutor(new DuelCommand(this, duelRequestManager));
        getCommand("mute").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("ban").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("unmute").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("unban").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("staff").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("freeze").setExecutor(new fr.rammex.chasetag.lobby.command.StaffCommand(this));
        getCommand("podium").setExecutor(new PodiumCommand(this));
        getCommand("ready").setExecutor(new fr.rammex.chasetag.lobby.command.ReadyCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLobbyEvents(this.playerMongoRepository), this);
        Bukkit.getPluginManager().registerEvents(new PodiumListener(this), this);

        // Refresh podium after load
        Bukkit.getScheduler().runTaskLater(this, () -> this.podiumManager.refreshPodium(), 20L);

        getLogger().info("ChaseTagLobby activé.");
    }

    @Override
    public void onDisable() {
        if (podiumManager != null) podiumManager.clearEntities();
        if (discordBot != null) discordBot.shutdown();
        if (redisListener != null) redisListener.stop();
        if (jedisPool != null) jedisPool.close();
        if (tournamentManager != null) tournamentManager.save();
        if (mongoManager != null) mongoManager.close();
        PlayerManager.save();
        getLogger().info("ChaseTagLobby désactivé.");
    }

    public static ChaseTagLobby getInstance() { return instance; }
    public JedisPool getJedisPool() { return jedisPool; }
    public MongoManager getMongoManager() { return mongoManager; }
    public PlayerMongoRepository getPlayerMongoRepository() { return playerMongoRepository; }
    public PterodactylClient getPterodactylClient() { return pterodactylClient; }
    public GameManager getGameManager() { return gameManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public DuelRequestManager getDuelRequestManager() { return duelRequestManager; }
    public DiscordBot getDiscordBot() { return discordBot; }
    public fr.rammex.chasetag.lobby.staff.StaffManager getStaffManager() { return staffManager; }
    public fr.rammex.chasetag.lobby.staff.StaffModeManager getStaffModeManager() { return staffModeManager; }
    public PodiumManager getPodiumManager() { return podiumManager; }
}
