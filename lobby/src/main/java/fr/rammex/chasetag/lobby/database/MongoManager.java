package fr.rammex.chasetag.lobby.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoManager {

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoManager(String uri, String databaseName) {
        this.mongoClient = MongoClients.create(uri);
        this.database = mongoClient.getDatabase(databaseName);
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
