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
        String finalUri = uri;
        if (uri == null || uri.isBlank() || (!uri.toLowerCase().startsWith("mongodb://") && !uri.toLowerCase().startsWith("mongodb+srv://"))) {
            finalUri = "mongodb://localhost:27017";
        }
        
        String sanitizedUri = sanitizeConnectionString(finalUri);
        try {
            this.mongoClient = MongoClients.create(sanitizedUri);
        } catch (Exception e) {
            try {
                // Seconde tentative avec l'URI originale si la sanitization a échoué
                this.mongoClient = MongoClients.create(finalUri);
            } catch (Exception e2) {
                // Fallback ultime
                this.mongoClient = MongoClients.create("mongodb://localhost:27017");
            }
        }
        this.database = mongoClient.getDatabase(databaseName);
    }

    private static String sanitizeConnectionString(String uri) {
        if (uri == null || uri.isEmpty()) return uri;
        
        String lowerUri = uri.toLowerCase();
        if (!lowerUri.startsWith("mongodb://") && !lowerUri.startsWith("mongodb+srv://")) return uri;

        int schemeEnd = uri.indexOf("://") + 3;
        
        // On cherche le DERNIER '@' avant les options (?) pour bien séparer les credentials de l'hôte
        int optionsIndex = uri.indexOf('?');
        int searchLimit = (optionsIndex != -1) ? optionsIndex : uri.length();
        int atIndex = uri.lastIndexOf('@', searchLimit);
        
        if (atIndex < 0 || atIndex < schemeEnd) return uri;

        String userInfo = uri.substring(schemeEnd, atIndex);
        if (userInfo.isEmpty() || !userInfo.contains(":")) return uri;

        // On sépare au PREMIER ':' pour le username:password
        int colonIndex = userInfo.indexOf(':');
        String username = userInfo.substring(0, colonIndex);
        String password = userInfo.substring(colonIndex + 1);

        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.name()).replace("+", "%20");
            String encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8.name()).replace("+", "%20");
            return uri.substring(0, schemeEnd) + encodedUser + ":" + encodedPass + uri.substring(atIndex);
        } catch (Exception e) {
            return uri;
        }
    }

    public void close() {
        if (mongoClient != null) mongoClient.close();
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
