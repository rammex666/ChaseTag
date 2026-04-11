package fr.rammex.chaseTag.game;

import fr.rammex.chaseTag.game.arena.Arena;
import fr.rammex.chaseTag.game.arena.ArenaManager;
import fr.rammex.chaseTag.game.arena.creation.event.ArenaCreationEvent;
import fr.rammex.chaseTag.game.command.ArenaCommand;
import fr.rammex.chaseTag.game.command.GameTestDevCommand;
import fr.rammex.chaseTag.game.command.RoleCommand;
import fr.rammex.chaseTag.game.command.TestGameCommand;
import fr.rammex.chaseTag.game.game.GameManager;
import fr.rammex.chaseTag.game.game.ScoreboardManager;
import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.events.PlayerListener;
import fr.rammex.chaseTag.game.player.events.PlayerMovementListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import fr.rammex.chaseTag.game.redis.GameRedisListener;
import fr.rammex.chaseTag.game.redis.GameRedisPublisher;
import fr.rammex.chaseTag.game.timer.TimerManager;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

public final class ChaseTag extends JavaPlugin {
    private static ChaseTag instance;
    private PlayerManager playerManager;
    private ArenaManager arenaManager;
    private JedisPool jedisPool;
    private GameRedisPublisher redisPublisher;
    private GameRedisListener redisListener;
    private TimerManager timerManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;


    private String serverId;
    private String host;
    private int port;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.serverId = getConfig().getString("server-id", "game-1");
        this.host = getConfig().getString("host", "localhost");
        this.port = getConfig().getInt("port", 25565);

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

        this.redisPublisher = new GameRedisPublisher(jedisPool, serverId, host, port);
        this.redisListener = new GameRedisListener(this);

        this.playerManager = new PlayerManager();
        PlayerManager.init(this.getDataFolder());

        this.arenaManager = new ArenaManager();
        ArenaManager.init(this.getDataFolder());

        this.timerManager = new TimerManager();
        this.gameManager = new GameManager(this);
        this.scoreboardManager = new ScoreboardManager(this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, 
    () -> timerManager.update(), 0, 1);
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
    () -> scoreboardManager.updateAll(), 0, 20);

        redisPublisher.publishServerReady();
        redisListener.start();

        getCommand("testgame").setExecutor(new TestGameCommand());
        getCommand("gametestdev").setExecutor(new GameTestDevCommand());
        getCommand("arena").setExecutor(new ArenaCommand());
        RoleCommand roleCommand = new RoleCommand();
        getCommand("setplayer").setExecutor(roleCommand);
        getCommand("setspec").setExecutor(roleCommand);
        getCommand("setstaff").setExecutor(roleCommand);

        loadEvents();

    }

    @Override
    public void onDisable() {
        PlayerManager.save();
        ArenaManager.save();
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public static ChaseTag getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ArenaManager getArenaManager(){return arenaManager;}

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public String getServerId() {
        return serverId;
    }

    public GameRedisPublisher getRedisPublisher() {
        return redisPublisher;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public void onGameFinished(String winnerUuid, String winnerName, List<String> playerUuids, java.util.Map<String, Integer> playerScores) {
        redisPublisher.publishGameEnd(winnerUuid, winnerName, playerUuids, playerScores);
    }

    private void loadEvents(){
        this.getServer().getPluginManager().registerEvents(new PlayerListener(),this);
        this.getServer().getPluginManager().registerEvents(new PlayerMovementListener(),this);
        this.getServer().getPluginManager().registerEvents(new ArenaCreationEvent(),this);
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }
}
/*
// Quand la partie se termine
List<String> playerUuids = players.stream()
    .map(p -> p.getUniqueId().toString())
    .collect(Collectors.toList());

ChaseTag.getInstance().onGameFinished(winner.getUniqueId().toString(), playerUuids);


*/

/*
timerManager.createTimer("match", 60)
    .onFinished(() -> {
        System.out.println("La partie est terminée!");
        // Votre code ici
    })
    .start();

*/