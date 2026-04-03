package fr.rammex.chaseTag.game.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {
    private static final Map<String, Arena> arenas = new HashMap<>();
    private static File file;
    private static Gson gson;

    public static void init(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        file = new File(dataFolder, "arena.json");

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Arena.class, new ArenaSerializer())
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

    public static void addArena(Arena arena) {
        arenas.put(arena.getId(), arena);
        save();
    }

    public static Arena getArena(String id) {
        return arenas.get(id);
    }

    public static void removeArena(String id) {
        arenas.remove(id);
        save();
    }

    public static void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(arenas, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Arena>>() {}.getType();
            Map<String, Arena> data = gson.fromJson(reader, type);

            if (data != null) {
                arenas.clear();
                arenas.putAll(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Arena> getAll() {
        return arenas;
    }
}
