package fr.rammex.chasetag.lobby.placeholder;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.rank.Rank;
import fr.rammex.chasetag.lobby.utils.ColorUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class LobbyPlaceholderExpansion extends PlaceholderExpansion {
    private final ChaseTagLobby plugin;

    public LobbyPlaceholderExpansion(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "chasetag";
    }

    @Override
    public String getAuthor() {
        if (plugin.getDescription().getAuthors().isEmpty()) {
            return "unknown";
        }
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "poule" -> plugin.getTournamentManager().getPouleLabel(player.getName());
            case "phase" -> plugin.getTournamentManager().getDisplayPhase(player.getName());
            case "grade_prefix" -> {
                fr.rammex.chasetag.lobby.player.Player stored = PlayerManager.getPlayer(player.getUniqueId().toString());
                if (stored == null || stored.getPlayerRole() == null) {
                    yield "";
                }
                Rank rank = stored.getPlayerRole();
                String prefix = rank.getPrefix();
                if (rank.getHex3() == null || rank.getHex3().isEmpty()) {
                    yield LegacyComponentSerializer.legacySection().serialize(
                            ColorUtils.gradient(prefix, rank.getHex1(), rank.getHex2())
                    );
                }
                yield LegacyComponentSerializer.legacySection().serialize(
                        ColorUtils.gradient(prefix, rank.getHex1(), rank.getHex2(), rank.getHex3())
                );
            }
            default -> "";
        };
    }
}