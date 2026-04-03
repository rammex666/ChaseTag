package fr.rammex.chaseTag.arena;

import com.google.gson.*;
import fr.rammex.chaseTag.player.Player;
import fr.rammex.chaseTag.player.Rank;
import fr.rammex.chaseTag.player.Role;

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

        return obj;
    }

    @Override
    public Arena deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String worldName = obj.get("worldName").getAsString();
        double x1 = obj.get("x1").getAsDouble();
        double x2 = obj.get("x2").getAsDouble();
        double y1 = obj.get("y1").getAsDouble();
        double y2 = obj.get("y2").getAsDouble();
        double z1 = obj.get("z1").getAsDouble();
        double z2 = obj.get("z2").getAsDouble();


        return new Arena(id, name, worldName, x1, x2, y1, y2, z1, z2);
    }
}