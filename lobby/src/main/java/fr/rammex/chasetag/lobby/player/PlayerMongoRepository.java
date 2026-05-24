package fr.rammex.chasetag.lobby.player;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import fr.rammex.chasetag.lobby.database.MongoManager;
import fr.rammex.chasetag.lobby.player.rank.Rank;

import org.bson.Document;

import java.util.Map;
import java.util.Optional;

public class PlayerMongoRepository {
    private final MongoCollection<Document> collection;

    public PlayerMongoRepository(MongoManager mongoManager) {
        this.collection = mongoManager.getDatabase().getCollection("player_info");
    }

    public void savePlayer(Player player) {
        if (player == null) {
            return;
        }

        Document document = new Document("uuid", player.getPlayerUUID())
                .append("name", player.getPlayerName())
                .append("rank", player.getPlayerRole().getId())
                .append("data", player.getPlayerData());

        collection.replaceOne(Filters.eq("name", player.getPlayerName()), document, new ReplaceOptions().upsert(true));
        collection.replaceOne(Filters.eq("uuid", player.getPlayerUUID()), document, new ReplaceOptions().upsert(true));
    }

    public Optional<Player> getPlayerByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        Document document = collection.find(Filters.eq("name", name)).first();
        return Optional.ofNullable(document).map(this::toPlayer);
    }

    public Optional<Player> getPlayerByUUID(String uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        Document document = collection.find(Filters.eq("uuid", uuid)).first();
        return Optional.ofNullable(document).map(this::toPlayer);
    }

    public void deletePlayerByName(String name) {
        if (name == null) {
            return;
        }
        collection.deleteOne(Filters.eq("name", name));
    }

    public void deletePlayerByUUID(String uuid) {
        if (uuid == null) {
            return;
        }
        collection.deleteOne(Filters.eq("uuid", uuid));
    }

    public java.util.List<Player> getAllPlayers() {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (Document document : collection.find()) {
            players.add(toPlayer(document));
        }
        return players;
    }

    public java.util.List<Player> getTopPlayers(String field, int limit) {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (Document document : collection.find().sort(new Document("data." + field, -1)).limit(limit)) {
            players.add(toPlayer(document));
        }
        return players;
    }

    public java.util.List<Player> getWhitelistedPlayers() {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (Document document : collection.find(Filters.eq("data.whitelisted", true))) {
            players.add(toPlayer(document));
        }
        return players;
    }

    private Player toPlayer(Document document) {
        if (document == null) {
            return null;
        }
        String uuid = document.getString("uuid");
        String name = document.getString("name");
        String rankId = document.getString("rank");
        Map<String, Object> data = document.get("data", Map.class);

        Player player = new Player(uuid, name, fr.rammex.chasetag.lobby.player.rank.Rank.getRoleFromID(rankId));
        if (data != null) {
            player.setPlayerData(data);
        }
        return player;
    }
}
