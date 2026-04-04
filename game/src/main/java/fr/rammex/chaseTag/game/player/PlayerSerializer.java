package fr.rammex.chaseTag.game.player;

import com.google.gson.*;
import java.lang.reflect.Type;

public class PlayerSerializer implements JsonSerializer<Player>, JsonDeserializer<Player> {

    @Override
    public JsonElement serialize(Player player, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("uuid", player.getPlayerUUID());
        obj.addProperty("role", player.getPlayerRole().getId());

        return obj;
    }

    @Override
    public Player deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        String uuid = obj.get("uuid").getAsString();
        Role role = Role.getRoleFromID(obj.get("role").getAsString());

        return new Player(uuid,role);
    }
}