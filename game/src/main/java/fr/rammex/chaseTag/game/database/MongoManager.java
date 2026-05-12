package fr.rammex.chaseTag.game.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MongoManager {

    private MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoManager(String uri, String databaseName) {
        String sanitizedUri = sanitizeConnectionString(uri);
        try {
            this.mongoClient = MongoClients.create(sanitizedUri);
        } catch (IllegalArgumentException exception) {
            this.mongoClient = MongoClients.create(uri);
        }
        this.database = mongoClient.getDatabase(databaseName);
    }

    private static String sanitizeConnectionString(String uri) {
        if (uri == null) return null;
        String lowerUri = uri.toLowerCase();
        if (!lowerUri.startsWith("mongodb://") && !lowerUri.startsWith("mongodb+srv://")) return uri;
        int schemeEnd = uri.indexOf("://") + 3;
        int atIndex = uri.indexOf('@', schemeEnd);
        if (atIndex < 0) return uri;
        return uri; // Simple return for now, the lobby version was more complex
    }

    public void close() {
        if (mongoClient != null) mongoClient.close();
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
