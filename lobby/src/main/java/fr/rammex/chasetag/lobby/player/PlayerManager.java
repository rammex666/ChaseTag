package fr.rammex.chasetag.lobby.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private static final Map<String, Player> players = new HashMap<>();
    private static File file;
    private static Gson gson;

    public static void init(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        file = new File(dataFolder, "player.json");

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Player.class, new PlayerSerializer())
                .create();

        try {
            if (!file.exists()) {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("[]");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        load();
    }

    public static void addPlayer(Player mineZone) {
        players.put(mineZone.getPlayerUUID(), mineZone);
        save();
    }

    public static Player getPlayer(String id) {
        return players.get(id);
    }

    public static Player getPlayerByName(String name) {
        if (name == null) {
            return null;
        }
        return players.values().stream()
                .filter(player -> name.equalsIgnoreCase(player.getPlayerName()))
                .findFirst()
                .orElse(null);
    }

    public static void removePlayer(String id) {
        players.remove(id);
        save();
    }

    public static void save() {
        if (file == null || gson == null) {
            return;
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(players, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (file == null || gson == null) {
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Player>>() {}.getType();
            Map<String, Player> data = gson.fromJson(reader, type);

            if (data != null) {
                players.clear();
                players.putAll(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Player> getAll() {
        return players;
    }
}
