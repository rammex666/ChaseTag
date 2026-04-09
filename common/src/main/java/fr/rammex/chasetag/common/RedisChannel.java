package fr.rammex.chasetag.common;

public final class RedisChannel {

    private RedisChannel() {}

    // Publié par chasetag-game quand le serveur est prêt à recevoir des joueurs
    public static final String SERVER_READY = "chasetag:server:ready";

    // Publié par chasetag-game quand la partie est terminée
    public static final String GAME_END = "chasetag:game:end";

    // Publié par chasetag-lobby pour demander le retour des joueurs
    public static final String SEND_TO_LOBBY = "chasetag:send:lobby";

    // Hash Redis : serverId -> ServerState (STARTING, WAITING, PLAYING, ENDING)
    public static final String SERVERS_MAP = "chasetag:servers";

    // Hash Redis : serverId -> port
    public static final String SERVERS_PORT = "chasetag:servers:port";

    // Hash Redis : playerUuid -> points
    public static final String GLOBAL_POINTS = "chasetag:points:global";

    // List Redis : queue des joueurs en attente de partie
    public static final String PLAYER_QUEUE = "chasetag:queue";
}
