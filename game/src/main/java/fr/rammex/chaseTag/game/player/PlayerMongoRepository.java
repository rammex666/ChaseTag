package fr.rammex.chaseTag.game.player;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.rammex.chaseTag.game.database.MongoManager;
import org.bson.Document;

import java.util.Map;
import java.util.Optional;

public class PlayerMongoRepository {
    private final MongoCollection<Document> collection;

    public PlayerMongoRepository(MongoManager mongoManager) {
        this.collection = mongoManager.getDatabase().getCollection("player_info");
    }

    public Optional<PlayerProfile> getPlayerByUUID(String uuid) {
        if (uuid == null) return Optional.empty();
        Document document = collection.find(Filters.eq("uuid", uuid)).first();
        return Optional.ofNullable(document).map(this::toProfile);
    }

    private PlayerProfile toProfile(Document document) {
        if (document == null) return null;
        Map<String, Object> data = document.get("data", Map.class);
        PlayerProfile profile = new PlayerProfile(document.getString("uuid"));
        if (data != null) {
            profile.setPlayerData(data);
        }
        return profile;
    }
}
