package fr.rammex.chaseTag.game.redis;

import fr.rammex.chasetag.common.MessageSerializer;
import fr.rammex.chasetag.common.RedisChannel;
import fr.rammex.chasetag.common.message.GameEndMessage;
import fr.rammex.chasetag.common.message.ServerReadyMessage;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class GameRedisPublisher {

    private final JedisPool jedisPool;
    private final String serverId;
    private final String host;
    private final int port;

    public GameRedisPublisher(JedisPool jedisPool, String serverId,
                              String host, int port) {
        this.jedisPool = jedisPool;
        this.serverId = serverId;
        this.host = host;
        this.port = port;
    }

    // Appelé au onEnable() du plugin, quand le serveur est prêt
    public void publishServerReady() {
        ServerReadyMessage msg = new ServerReadyMessage(serverId, port, host);
        publish(RedisChannel.SERVER_READY, MessageSerializer.serialize(msg));
    }

    // Appelé quand la partie se termine
    public void publishGameEnd(String winnerUuid, String winnerName, List<String> playerUuids, java.util.Map<String, Integer> playerScores, java.util.Map<String, Integer> bestHunterTimes, java.util.Map<String, Integer> bestRunnerTimes) {
        GameEndMessage msg = new GameEndMessage(serverId, winnerUuid, winnerName, playerUuids, playerScores, bestHunterTimes, bestRunnerTimes);
        publish(RedisChannel.GAME_END, MessageSerializer.serialize(msg));
    }

    private void publish(String channel, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}