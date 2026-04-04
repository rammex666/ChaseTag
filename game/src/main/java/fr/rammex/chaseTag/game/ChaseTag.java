package fr.rammex.chaseTag.game;

import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.events.PlayerListener;

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
    private JedisPool jedisPool;
    private GameRedisPublisher redisPublisher;
    private GameRedisListener redisListener;
    private TimerManager timerManager;


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

        this.timerManager = new TimerManager();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, 
    () -> timerManager.update(), 0, 1);

        redisPublisher.publishServerReady();
        redisListener.start();

        loadEvents();

    }

    @Override
    public void onDisable() {
        PlayerManager.save();
        if (jedisPool != null) {
            jedisPool.close();
        }
        if (redisListener != null) {
            // Assuming GameRedisListener has a stop method, but it doesn't, so maybe add one or just let it die
        }
    }

    public static ChaseTag getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public String getServerId() {
        return serverId;
    }

    public GameRedisPublisher getRedisPublisher() {
        return redisPublisher;
    }

    public void onGameFinished(String winnerUuid, List<String> playerUuids) {
        redisPublisher.publishGameEnd(winnerUuid, playerUuids);
    }

    private void loadEvents(){
        this.getServer().getPluginManager().registerEvents(new PlayerListener(),this);
    }

    private TimerManager getTimerManager() {
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