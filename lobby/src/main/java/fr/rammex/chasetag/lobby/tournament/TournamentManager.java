package fr.rammex.chasetag.lobby.tournament;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.database.MongoManager;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class TournamentManager {
    private final ChaseTagLobby plugin;
    private final MongoManager mongoManager;
    private final Map<String, Integer> playerPoule = new HashMap<>();
    private final Set<String> eliminatedPlayers = new HashSet<>();
    private final List<PhaseConfig> phases = new ArrayList<>();
    private final List<TournamentMatch> matches = new ArrayList<>();
    private String currentPhase = "Phase 1";
    private Mode mode = Mode.TOURNAMENT;

    public enum Mode {
        TOURNAMENT,
        PRACTICE
    }

    public TournamentManager(ChaseTagLobby plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        loadPhaseConfigs();
        load();
        ensureCurrentPhaseValid();
        initializePhaseMatchesIfNeeded();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.TOURNAMENT : mode;
        save();
    }

    public boolean toggleMode() {
        this.mode = (this.mode == Mode.TOURNAMENT ? Mode.PRACTICE : Mode.TOURNAMENT);
        save();
        return this.mode == Mode.PRACTICE;
    }

    public boolean isPracticeMode() {
        return mode == Mode.PRACTICE;
    }

    private void loadPhaseConfigs() {
        phases.clear();
        if (!plugin.getConfig().isList("tournament.phases")) {
            return;
        }

        List<?> configPhases = plugin.getConfig().getList("tournament.phases");
        if (configPhases == null) {
            return;
        }

        for (Object entry : configPhases) {
            if (!(entry instanceof Map<?, ?> phaseMap)) {
                continue;
            }
            Object nameValue = phaseMap.get("name");
            Object typeValueObj = phaseMap.get("type");
            String name = nameValue == null ? "Phase 1" : String.valueOf(nameValue);
            String typeValue = typeValueObj == null ? "poule" : String.valueOf(typeValueObj);
            typeValue = typeValue.toUpperCase(Locale.ROOT);
            PhaseType type = typeValue.equals("BRACKET") ? PhaseType.BRACKET : PhaseType.POULE;
            int pouleCount = safeInt(phaseMap.get("pouleCount"));
            int playersPerPoule = safeInt(phaseMap.get("playersPerPoule"));
            int bracketSize = safeInt(phaseMap.get("bracketSize"));
            phases.add(new PhaseConfig(name, type, pouleCount, playersPerPoule, bracketSize));
        }

        if (phases.isEmpty()) {
            phases.add(new PhaseConfig("Phase 1", PhaseType.POULE, 8, 4, 0));
        }
    }

    private int safeInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public void setPlayerPoule(String playerName, int poule) {
        if (playerName == null || playerName.isBlank() || poule < 1 || poule > 8) {
            return;
        }
        playerPoule.put(playerName, poule);
        save();
    }

    public void unassignPlayer(String playerName) {
        if (playerName == null) {
            return;
        }
        playerPoule.remove(playerName);
        save();
    }

    public int getPlayerPoule(String playerName) {
        if (playerName == null) {
            return 0;
        }
        return playerPoule.getOrDefault(playerName, 0);
    }

    public String getPouleLabel(String playerName) {
        int poule = getPlayerPoule(playerName);
        return poule == 0 ? "Aucune" : "Poule " + poule;
    }

    public boolean isPlayerEliminated(String playerName) {
        return playerName != null && eliminatedPlayers.contains(playerName);
    }

    public void setPlayerEliminated(String playerName, boolean eliminated) {
        if (playerName == null) {
            return;
        }
        if (eliminated) {
            eliminatedPlayers.add(playerName);
        } else {
            eliminatedPlayers.remove(playerName);
        }
        save();
    }

    public Optional<PhaseConfig> getCurrentPhaseConfig() {
        return phases.stream()
                .filter(phase -> phase.getName().equals(currentPhase))
                .findFirst();
    }

    public Optional<PhaseConfig> getPhaseByName(String phaseName) {
        return phases.stream()
                .filter(phase -> phase.getName().equals(phaseName))
                .findFirst();
    }

    public List<PhaseConfig> getPhaseConfigs() {
        return List.copyOf(phases);
    }

    public void ensureCurrentPhaseValid() {
        if (getPhaseByName(currentPhase).isEmpty() && !phases.isEmpty()) {
            currentPhase = phases.get(0).getName();
        }
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public boolean setPhase(String phase) {
        if (phase == null || getPhaseByName(phase).isEmpty()) {
            return false;
        }
        this.currentPhase = phase;
        initializePhaseMatchesIfNeeded();
        save();
        return true;
    }

    public String nextPhase() {
        int index = -1;
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).getName().equals(currentPhase)) {
                index = i;
                break;
            }
        }
        if (index < 0 || index + 1 >= phases.size()) {
            return currentPhase;
        }
        currentPhase = phases.get(index + 1).getName();
        initializePhaseMatchesIfNeeded();
        save();
        return currentPhase;
    }

    public String getDisplayPhase(String playerName) {
        return isPlayerEliminated(playerName) ? "Éliminé" : currentPhase;
    }

    public List<String> getPlayersInPoule(int poule) {
        return playerPoule.entrySet().stream()
                .filter(entry -> entry.getValue() == poule)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int getPlayersCountInPoule(int poule) {
        return (int) playerPoule.values().stream().filter(value -> value == poule).count();
    }

    public List<String> getEliminatedPlayers() {
        return new ArrayList<>(eliminatedPlayers);
    }

    public List<String> getRemainingPlayers() {
        return playerPoule.keySet().stream()
                .filter(playerName -> !eliminatedPlayers.contains(playerName))
                .collect(Collectors.toList());
    }

    public List<TournamentMatch> getMatchesForPool(String phase, int pool) {
        return matches.stream()
                .filter(match -> match.getPhase().equals(phase) && match.getPool() == pool)
                .collect(Collectors.toList());
    }

    public List<TournamentMatch> getMatchesForPhase(String phase) {
        return matches.stream()
                .filter(match -> match.getPhase().equals(phase))
                .collect(Collectors.toList());
    }

    public Optional<TournamentMatch> getMatch(String phase, int pool, int matchId) {
        return matches.stream()
                .filter(match -> match.getPhase().equals(phase) && match.getPool() == pool && match.getMatchId() == matchId)
                .findFirst();
    }

    public Optional<TournamentMatch> getMatch(String phase, int matchId) {
        return matches.stream()
                .filter(match -> match.getPhase().equals(phase) && match.getMatchId() == matchId)
                .findFirst();
    }

    public void assignPlayerToMatch(String phase, int matchId, String playerName) {
        assignPlayerToMatch(phase, 0, matchId, playerName);
    }

    public void assignPlayerToMatch(String phase, int pool, int matchId, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        removePlayerFromPhaseMatches(phase, playerName);
        TournamentMatch match = getMatch(phase, pool, matchId).orElseGet(() -> createMatch(phase, pool, matchId));
        TournamentMatch updatedMatch;
        if (match.getPlayer1() == null) {
            updatedMatch = new TournamentMatch(phase, match.getPool(), matchId, playerName, match.getPlayer2String(), match.getEggId(), match.getMapName());
        } else if (match.getPlayer2() == null) {
            updatedMatch = new TournamentMatch(phase, match.getPool(), matchId, match.getPlayer1String(), playerName, match.getEggId(), match.getMapName());
        } else {
            updatedMatch = new TournamentMatch(phase, match.getPool(), matchId, match.getPlayer1String(), playerName, match.getEggId(), match.getMapName());
        }
        replaceMatch(match, updatedMatch);
        save();
        maybeStartMatch(updatedMatch);
    }

    public void setMatchMap(String phase, int pool, int matchId, int eggId, String mapName) {
        if (phase == null || phase.isBlank()) {
            return;
        }
        TournamentMatch match = getMatch(phase, pool, matchId).orElseGet(() -> createMatch(phase, pool, matchId));
        TournamentMatch updatedMatch = new TournamentMatch(
                phase,
                match.getPool(),
                matchId,
                match.getPlayer1String(),
                match.getPlayer2String(),
                eggId,
                mapName
        );
        replaceMatch(match, updatedMatch);
        save();
        maybeStartMatch(updatedMatch);
    }

    private void maybeStartMatch(TournamentMatch match) {
        if (match == null || match.getPlayer1() == null || match.getPlayer2() == null || !match.hasMap()) {
            return;
        }
        org.bukkit.entity.Player player1 = Bukkit.getPlayerExact(match.getPlayer1());
        org.bukkit.entity.Player player2 = Bukkit.getPlayerExact(match.getPlayer2());
        if (player1 == null || player2 == null) {
            return;
        }
        if (plugin.getGameManager().getSessionByPlayer(player1.getUniqueId()) != null
                || plugin.getGameManager().getSessionByPlayer(player2.getUniqueId()) != null) {
            return;
        }

        var session = plugin.getGameManager().createSession(player1.getUniqueId(), fr.rammex.chasetag.lobby.game.GameSession.GameType.TOURNAMENT);
        session.setMap(match.getEggId(), match.getMapName());
        boolean joined = plugin.getGameManager().joinSession(session.getSessionId(), player2.getUniqueId());
        if (joined) {
            player1.sendMessage("§aMatch de tournoi en cours de démarrage : §e" + match.getMapName());
            player2.sendMessage("§aMatch de tournoi en cours de démarrage : §e" + match.getMapName());
        }
    }

    private void removePlayerFromPhaseMatches(String phase, String playerName) {
        for (TournamentMatch match : new ArrayList<>(matches)) {
            if (!match.getPhase().equals(phase)) {
                continue;
            }
            if (match.containsPlayer(playerName)) {
                String player1 = match.getPlayer1() != null && !match.getPlayer1().equals(playerName) ? match.getPlayer1String() : "";
                String player2 = match.getPlayer2() != null && !match.getPlayer2().equals(playerName) ? match.getPlayer2String() : "";
                replaceMatch(match, new TournamentMatch(match.getPhase(), match.getPool(), match.getMatchId(), player1, player2, match.getEggId(), match.getMapName()));
            }
        }
    }

    private TournamentMatch createMatch(String phase, int pool, int matchId) {
        TournamentMatch match = new TournamentMatch(phase, pool, matchId, "", "", 0, "");
        matches.add(match);
        return match;
    }

    private void replaceMatch(TournamentMatch oldMatch, TournamentMatch newMatch) {
        matches.remove(oldMatch);
        matches.add(newMatch);
    }

    public void save() {
        Document document = new Document("_id", "tournament")
                .append("phase", currentPhase)
                .append("mode", mode.name())
                .append("poules", new Document(playerPoule))
                .append("eliminated", new ArrayList<>(eliminatedPlayers))
                .append("matches", matches.stream().map(TournamentMatch::toDocument).collect(Collectors.toList()));

        getCollection().replaceOne(Filters.eq("_id", "tournament"), document, new ReplaceOptions().upsert(true));
    }

    private void load() {
        Document document = getCollection().find(Filters.eq("_id", "tournament")).first();
        if (document == null) {
            save();
            return;
        }

        if (document.containsKey("phase")) {
            currentPhase = document.getString("phase");
        }
        if (document.containsKey("mode")) {
            String modeString = document.getString("mode");
            try {
                mode = Mode.valueOf(modeString);
            } catch (IllegalArgumentException | NullPointerException ignored) {
                mode = Mode.TOURNAMENT;
            }
        }

        Document poulesDocument = document.get("poules", Document.class);
        if (poulesDocument != null) {
            for (String key : poulesDocument.keySet()) {
                Object value = poulesDocument.get(key);
                if (value instanceof Number number) {
                    playerPoule.put(key, number.intValue());
                }
            }
        }

        List<String> eliminated = document.get("eliminated", List.class);
        if (eliminated != null) {
            eliminatedPlayers.clear();
            eliminatedPlayers.addAll(eliminated);
        }

        List<Document> matchDocuments = document.get("matches", List.class);
        if (matchDocuments != null) {
            matches.clear();
            for (Document matchDocument : matchDocuments) {
                TournamentMatch match = TournamentMatch.fromDocument(matchDocument);
                if (match != null) {
                    matches.add(match);
                }
            }
        }
    }

    private void initializePhaseMatchesIfNeeded() {
        Optional<PhaseConfig> phaseConfig = getCurrentPhaseConfig();
        if (phaseConfig.isEmpty()) {
            return;
        }

        String phaseName = phaseConfig.get().getName();
        if (!getMatchesForPhase(phaseName).isEmpty()) {
            return;
        }

        if (phaseConfig.get().isPoule()) {
            int pouleCount = Math.max(1, phaseConfig.get().getPouleCount());
            int matchesPerPoule = Math.max(1, phaseConfig.get().getPlayersPerPoule() / 2);
            for (int pool = 1; pool <= pouleCount; pool++) {
                for (int matchId = 1; matchId <= matchesPerPoule; matchId++) {
                    matches.add(new TournamentMatch(phaseName, pool, matchId, "", ""));
                }
            }
        } else {
            int matchCount = Math.max(1, phaseConfig.get().getBracketSize() / 2);
            for (int matchId = 1; matchId <= matchCount; matchId++) {
                matches.add(new TournamentMatch(phaseName, 0, matchId, "", ""));
            }
        }
        save();
    }

    private MongoCollection<Document> getCollection() {
        return mongoManager.getDatabase().getCollection("tournament");
    }
}