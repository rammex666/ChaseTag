package fr.rammex.chaseTag.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import fr.rammex.chaseTag.velocity.listener.PlayerConnectionListener;
import fr.rammex.chaseTag.velocity.redis.VelocityRedisListener;
import fr.rammex.chaseTag.velocity.config.VelocityConfig;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetSocketAddress;
import java.nio.file.Path;

@Plugin(
    id = "chasetag-velocity",
    name = "ChaseTagVelocity",
    version = "1.0.0",
    authors = {"rammex"}
)
public class ChaseTagVelocity {

    private static ChaseTagVelocity instance;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private JedisPool jedisPool;
    private VelocityRedisListener redisListener;
    private VelocityConfig config;

    @Inject
    public ChaseTagVelocity(ProxyServer proxy, Logger logger,
                            @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = new VelocityConfig(dataDirectory);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        this.jedisPool = new JedisPool(
            poolConfig,
            config.getRedisHost(),
            config.getRedisPort()
        );

        this.redisListener = new VelocityRedisListener(this);
        this.redisListener.start();

        // Dans ChaseTagVelocity.onProxyInitialize(), après le redisListener :
        proxy.getEventManager().register(this, new PlayerConnectionListener(this));

        logger.info("ChaseTagVelocity activé.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redisListener != null) redisListener.stop();
        if (jedisPool != null) jedisPool.close();
        logger.info("ChaseTagVelocity désactivé.");
    }

    // Enregistrer un game server dynamiquement dans Velocity
    // Appelé par VelocityRedisListener quand SERVER_READY est reçu
    public void registerGameServer(String serverName, String host, int port) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        ServerInfo info = new ServerInfo(serverName, address);
        proxy.registerServer(info);
        logger.info("Serveur enregistré dans Velocity : " + serverName + " -> " + host + ":" + port);
    }

    // Désenregistrer quand la partie est terminée
    public void unregisterGameServer(String serverName) {
        proxy.getServer(serverName).ifPresent(s -> proxy.unregisterServer(s.getServerInfo()));
        logger.info("Serveur retiré de Velocity : " + serverName);
    }

    public static ChaseTagVelocity getInstance() { return instance; }
    public ProxyServer getProxy() { return proxy; }
    public Logger getLogger() { return logger; }
    public JedisPool getJedisPool() { return jedisPool; }
    public VelocityConfig getConfig() { return config; }
}