package fr.rammex.chaseTag.lobby.pterodactyl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;

public class PterodactylClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final String apiUrl;
    private final String apiKey;
    private final int eggId;
    private final OkHttpClient http = new OkHttpClient();

    public PterodactylClient(String apiUrl, String apiKey, int eggId) {
        this.apiUrl = apiUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.eggId = eggId;
    }

    public ServerInfo createServer(String gameId) throws IOException {
        JsonObject env = new JsonObject();
        env.addProperty("GAME_ID", gameId);
        env.addProperty("SERVER_MEMORY", "512");

        JsonObject limits = new JsonObject();
        limits.addProperty("memory", 512);
        limits.addProperty("swap", 0);
        limits.addProperty("disk", 1024);
        limits.addProperty("io", 500);
        limits.addProperty("cpu", 100);

        JsonObject allocation = new JsonObject();
        allocation.addProperty("auto", true);

        JsonObject body = new JsonObject();
        body.addProperty("name", "chasetag-" + gameId);
        body.addProperty("egg", eggId);
        body.addProperty("docker_image", "ghcr.io/pterodactyl/yolks:java_21");
        body.addProperty("startup",
            "java -Xms128M -Xmx{{SERVER_MEMORY}}M -jar server.jar nogui");
        body.addProperty("skip_scripts", false);
        body.add("environment", env);
        body.add("limits", limits);
        body.add("allocation", allocation);

        Request request = new Request.Builder()
            .url(apiUrl + "/api/application/servers")
            .post(RequestBody.create(body.toString(), JSON))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Pterodactyl API error " + response.code()
                    + " : " + responseBody);
            }

            JsonObject result = JsonParser.parseString(responseBody)
                .getAsJsonObject()
                .getAsJsonObject("attributes");

            String serverId = result.get("identifier").getAsString();
            int port = result.getAsJsonObject("relationships")
                .getAsJsonObject("allocations")
                .getAsJsonArray("data")
                .get(0).getAsJsonObject()
                .getAsJsonObject("attributes")
                .get("port").getAsInt();

            return new ServerInfo(serverId, port);
        }
    }

    public void deleteServer(String serverId) throws IOException {
        Request request = new Request.Builder()
            .url(apiUrl + "/api/application/servers/" + serverId + "/force")
            .delete()
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw new IOException("Erreur suppression serveur : " + response.code());
            }
        }
    }

    public record ServerInfo(String serverId, int port) {}
}