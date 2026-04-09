package fr.rammex.chaseTag.game.arena;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;

public class ArenaSerializer implements JsonSerializer<Arena>, JsonDeserializer<Arena> {

    @Override
    public JsonElement serialize(Arena arena, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("id", arena.getId());
        obj.addProperty("name", arena.getName());
        obj.addProperty("worldname", arena.getWorldName());
        obj.addProperty("x1", arena.getX1());
        obj.addProperty("x2", arena.getX2());
        obj.addProperty("y1", arena.getY1());
        obj.addProperty("y2", arena.getY2());
        obj.addProperty("z1", arena.getZ1());
        obj.addProperty("z2", arena.getZ2());
        obj.addProperty("maxWoolTowerHeight", arena.getMaxWoolTowerHeight());

        if (arena.getBlueSpawn() != null) obj.add("blueSpawn", serializeLocation(arena.getBlueSpawn()));
        if (arena.getRedSpawn() != null) obj.add("redSpawn", serializeLocation(arena.getRedSpawn()));
        if (arena.getSpecSpawn() != null) obj.add("specSpawn", serializeLocation(arena.getSpecSpawn()));

        return obj;
    }

    private JsonObject serializeLocation(Location loc) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", loc.getWorld().getName());
        obj.addProperty("x", loc.getX());
        obj.addProperty("y", loc.getY());
        obj.addProperty("z", loc.getZ());
        obj.addProperty("yaw", loc.getYaw());
        obj.addProperty("pitch", loc.getPitch());
        return obj;
    }

    private Location deserializeLocation(JsonElement json) {
        if (json == null || !json.isJsonObject()) return null;
        JsonObject obj = json.getAsJsonObject();
        World world = Bukkit.getWorld(obj.get("world").getAsString());
        double x = obj.get("x").getAsDouble();
        double y = obj.get("y").getAsDouble();
        double z = obj.get("z").getAsDouble();
        float yaw = obj.get("yaw").getAsFloat();
        float pitch = obj.get("pitch").getAsFloat();
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public Arena deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String worldName = obj.has("worldname") ? obj.get("worldname").getAsString() : obj.get("worldName").getAsString();
        double x1 = obj.get("x1").getAsDouble();
        double x2 = obj.get("x2").getAsDouble();
        double y1 = obj.get("y1").getAsDouble();
        double y2 = obj.get("y2").getAsDouble();
        double z1 = obj.get("z1").getAsDouble();
        double z2 = obj.get("z2").getAsDouble();

        Arena arena = new Arena(id, name, worldName, x1, x2, y1, y2, z1, z2);
        
        if (obj.has("maxWoolTowerHeight")) arena.setMaxWoolTowerHeight(obj.get("maxWoolTowerHeight").getAsInt());
        if (obj.has("blueSpawn")) arena.setBlueSpawn(deserializeLocation(obj.get("blueSpawn")));
        if (obj.has("redSpawn")) arena.setRedSpawn(deserializeLocation(obj.get("redSpawn")));
        if (obj.has("specSpawn")) arena.setSpecSpawn(deserializeLocation(obj.get("specSpawn")));

        return arena;
    }
}