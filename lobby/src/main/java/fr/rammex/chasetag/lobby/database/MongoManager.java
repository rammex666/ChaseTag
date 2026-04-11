package fr.rammex.chasetag.lobby.database;

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
            if (!sanitizedUri.equals(uri)) {
                this.mongoClient = MongoClients.create(sanitizedUri);
            } else {
                throw exception;
            }
        }
        this.database = mongoClient.getDatabase(databaseName);
    }

    private static String sanitizeConnectionString(String uri) {
        if (uri == null) {
            return uri;
        }

        String lowerUri = uri.toLowerCase();
        if (!lowerUri.startsWith("mongodb://") && !lowerUri.startsWith("mongodb+srv://")) {
            return uri;
        }

        int schemeEnd = uri.indexOf("://") + 3;
        int atIndex = uri.indexOf('@', schemeEnd);
        if (atIndex < 0) {
            return uri;
        }

        String userInfo = uri.substring(schemeEnd, atIndex);
        if (userInfo.isEmpty() || !userInfo.contains(":")) {
            return uri;
        }

        String[] parts = userInfo.split(":", 2);
        if (parts.length < 2) {
            return uri;
        }

        String username = encodeValue(parts[0]);
        String password = encodeValue(parts[1]);
        return uri.substring(0, schemeEnd) + username + ":" + password + uri.substring(atIndex);
    }

    private static String encodeValue(String value) {
        if (value == null) {
            return null;
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
