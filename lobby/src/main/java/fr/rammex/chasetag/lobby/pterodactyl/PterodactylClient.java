package fr.rammex.chasetag.lobby.pterodactyl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.rammex.chasetag.lobby.ChaseTagLobby;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.io.IOException;

public class PterodactylClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final int pterodactylUserId = 1;
    private final int nodeId = 1;
    private final String apiUrl;
    private final String apiKey;
    private final String clientApiKey = "ptlc_XnLysdJEyrMTaTeyd99uod02x5in1VCrJjD8rQLnUrg";
    private final int eggId;
    private final OkHttpClient http = new OkHttpClient();

    public PterodactylClient(String apiUrl, String apiKey, int eggId) {
        this.apiUrl = apiUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.eggId = eggId;
    }

    public ServerInfo createServer(String gameId) throws IOException {
        return createServer(gameId, eggId);
    }

    public ServerInfo createServer(String gameId, int eggId) throws IOException {
        if (eggId <= 0) {
            eggId = this.eggId;
        }

        int allocationId = getAvailableAllocationId();
        int port = getAllocationPort(allocationId);

        String externalHost = ChaseTagLobby.getInstance()
                .getConfig().getString("pterodactyl.external-host", "localhost");

        JsonObject env = new JsonObject();
        env.addProperty("GAME_ID", gameId);
        env.addProperty("GAME_HOST", externalHost);
        env.addProperty("GAME_PORT", port);
        env.addProperty("SERVER_JARFILE", "server.jar");
        env.addProperty("DOWNLOAD_URL", "http://play.ownedcup.fr:25599/server.tar.gz");

        JsonObject limits = new JsonObject();
        limits.addProperty("memory", 2048);
        limits.addProperty("swap", 0);
        limits.addProperty("disk", 1024);
        limits.addProperty("io", 500);
        limits.addProperty("cpu", 150);

        JsonObject featureLimits = new JsonObject();
        featureLimits.addProperty("databases", 0);
        featureLimits.addProperty("backups", 0);
        featureLimits.addProperty("allocations", 1);

        JsonObject allocation = new JsonObject();
        allocation.addProperty("default", allocationId);

        JsonObject body = new JsonObject();
        body.addProperty("name", "chasetag-" + gameId);
        body.addProperty("egg", eggId);
        body.addProperty("user", pterodactylUserId);
        body.addProperty("docker_image", "ghcr.io/pterodactyl/yolks:java_21");
        body.addProperty("startup", "bash start.sh");
        body.addProperty("skip_scripts", false);
        body.add("environment", env);
        body.add("limits", limits);
        body.add("feature_limits", featureLimits);
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
            int internalId = result.get("id").getAsInt();

            ChaseTagLobby.getInstance().getLogger().info("Pterodactyl server created: id=" + serverId
                    + ", internalId=" + internalId
                    + ", allocationPort=" + port
                    + ", externalHost=" + externalHost);

            Bukkit.getScheduler().runTaskAsynchronously(ChaseTagLobby.getInstance(), () -> {
                try {
                    waitForInstallation(internalId);
                    startServer(serverId);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });

            return new ServerInfo(serverId, port, internalId);
        }
    }

    public void deleteServer(int numericId) throws IOException {
        Request request = new Request.Builder()
                .url(apiUrl + "/api/application/servers/" + numericId + "/force")
                .delete()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw new IOException("Erreur suppression serveur : " + response.code()
                        + " : " + response.body().string());
            }
            ChaseTagLobby.getInstance().getLogger().info("Serveur supprimé : #" + numericId);
        }
    }

    private void waitForInstallation(int internalId) throws IOException, InterruptedException {
        for (int i = 0; i < 60; i++) {
            Request request = new Request.Builder()
                    .url(apiUrl + "/api/application/servers/" + internalId)
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = http.newCall(request).execute()) {
                String bodyStr = response.body().string();
                JsonObject attrs = JsonParser.parseString(bodyStr)
                        .getAsJsonObject()
                        .getAsJsonObject("attributes");

                if (attrs == null) {
                    Thread.sleep(5000);
                    continue;
                }

                int installed = attrs
                        .getAsJsonObject("container")
                        .get("installed").getAsInt();

                if (installed == 1) return;
            }

            Thread.sleep(5000);
        }
        throw new IOException("Timeout : installation trop longue pour server#" + internalId);
    }

    public void startServer(String serverId) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("signal", "start");

        Request request = new Request.Builder()
                .url(apiUrl + "/api/client/servers/" + serverId + "/power")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Authorization", "Bearer " + clientApiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erreur start serveur : " + response.code()
                        + " : " + response.body().string());
            }
        }
    }

    private int getAvailableAllocationId() throws IOException {
        Request request = new Request.Builder()
                .url(apiUrl + "/api/application/nodes/" + nodeId + "/allocations?per_page=100")
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            String body = response.body().string();
            JsonArray data = JsonParser.parseString(body)
                    .getAsJsonObject()
                    .getAsJsonArray("data");

            for (JsonElement el : data) {
                JsonObject attrs = el.getAsJsonObject().getAsJsonObject("attributes");
                if (!attrs.get("assigned").getAsBoolean()) {
                    return attrs.get("id").getAsInt();
                }
            }
            throw new IOException("Aucune allocation disponible sur le node " + nodeId);
        }
    }

    private int getAllocationPort(int allocationId) throws IOException {
        Request request = new Request.Builder()
                .url(apiUrl + "/api/application/nodes/" + nodeId + "/allocations?per_page=100")
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            String body = response.body().string();
            JsonArray data = JsonParser.parseString(body)
                    .getAsJsonObject()
                    .getAsJsonArray("data");

            for (JsonElement el : data) {
                JsonObject attrs = el.getAsJsonObject().getAsJsonObject("attributes");
                if (attrs.get("id").getAsInt() == allocationId) {
                    return attrs.get("port").getAsInt();
                }
            }
            throw new IOException("Allocation introuvable : " + allocationId);
        }
    }

    public record ServerInfo(String serverId, int port, int numericId) {}
}