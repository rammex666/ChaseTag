package fr.rammex.chaseTag.velocity.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class VelocityConfig {

    private final Properties props = new Properties();

    public VelocityConfig(Path dataDirectory) {
        File configFile = dataDirectory.resolve("config.properties").toFile();

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            // Créer config par défaut
            try (InputStream in = getClass()
                    .getResourceAsStream("/config.properties")) {
                if (in != null) Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(configFile)) {
            props.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRedisHost() {
        return props.getProperty("redis.host", "localhost");
    }

    public int getRedisPort() {
        return Integer.parseInt(props.getProperty("redis.port", "6379"));
    }

    public String getLobbyServerName() {
        return props.getProperty("lobby.server-name", "lobby");
    }
}