package fr.rammex.chasetag.lobby.podium;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PodiumCommand implements CommandExecutor {
    private final ChaseTagLobby plugin;

    public PodiumCommand(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut exécuter cette commande.");
            return true;
        }

        if (!player.hasPermission("chasetag.admin")) {
            player.sendMessage(ColorUtils.format("&cVous n'avez pas la permission."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtils.format("&eUsage: /podium <set1|set2|set3|sethologram|refresh>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set1" -> {
                plugin.getPodiumManager().savePodiumLocation(0, player.getLocation());
                player.sendMessage(ColorUtils.format("&aPodium 1 défini !"));
            }
            case "set2" -> {
                plugin.getPodiumManager().savePodiumLocation(1, player.getLocation());
                player.sendMessage(ColorUtils.format("&aPodium 2 défini !"));
            }
            case "set3" -> {
                plugin.getPodiumManager().savePodiumLocation(2, player.getLocation());
                player.sendMessage(ColorUtils.format("&aPodium 3 défini !"));
            }
            case "sethologram" -> {
                plugin.getPodiumManager().saveHologramLocation(player.getLocation().add(0, 2, 0));
                player.sendMessage(ColorUtils.format("&aHologramme défini !"));
            }
            case "refresh" -> {
                plugin.getPodiumManager().refreshPodium();
                player.sendMessage(ColorUtils.format("&aPodium rafraîchi !"));
            }
            default -> player.sendMessage(ColorUtils.format("&cUsage: /podium <set1|set2|set3|sethologram|refresh>"));
        }

        return true;
    }
}
