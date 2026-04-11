package fr.rammex.chasetag.lobby.player;

import com.google.gson.*;
import fr.rammex.chasetag.lobby.player.rank.Rank;

import java.lang.reflect.Type;

public class PlayerSerializer implements JsonSerializer<Player>, JsonDeserializer<Player> {

    @Override
    public JsonElement serialize(Player player, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("uuid", player.getPlayerUUID());
        obj.addProperty("name", player.getPlayerName());
        obj.addProperty("rank", player.getPlayerRole().getId());
        obj.add("data", context.serialize(player.getPlayerData()));

        return obj;
    }

    @Override
    public Player deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        String uuid = obj.get("uuid").getAsString();
        String name = obj.has("name") ? obj.get("name").getAsString() : "";
        Rank role = Rank.getRoleFromID(obj.get("rank").getAsString());
        Player player = new Player(uuid, name, role);
        if (obj.has("data")) {
            player.setPlayerData(context.deserialize(obj.get("data"), player.getPlayerData().getClass()));
        }
        return player;
    }
}