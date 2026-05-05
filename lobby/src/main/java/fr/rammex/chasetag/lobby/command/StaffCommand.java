package fr.rammex.chasetag.lobby.command;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.rank.Rank;
import fr.rammex.chasetag.lobby.staff.Sanction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class StaffCommand implements CommandExecutor {

    private final ChaseTagLobby plugin;

    public StaffCommand(ChaseTagLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        // Vérifier si le joueur est staff
        fr.rammex.chasetag.lobby.player.Player ctPlayer = fr.rammex.chasetag.lobby.player.PlayerManager.getPlayer(player.getUniqueId().toString());
        if (ctPlayer == null || (ctPlayer.getPlayerRole() != Rank.Orga && ctPlayer.getPlayerRole() != Rank.Staff && ctPlayer.getPlayerRole() != Rank.RespStaff && ctPlayer.getPlayerRole() != Rank.Dev)) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (label.equalsIgnoreCase("staff")) {
            plugin.getStaffModeManager().toggleStaffMode(player);
            return true;
        }

        if (label.equalsIgnoreCase("freeze")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /freeze <joueur>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Joueur non trouvé.");
                return true;
            }
            plugin.getStaffModeManager().toggleFreeze(player, target);
            return true;
        }

        if (label.equalsIgnoreCase("unmute")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /unmute <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            plugin.getStaffManager().unmute(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Vous avez unmute " + ChatColor.AQUA + args[0] + ".");
            return true;
        }

        if (label.equalsIgnoreCase("unban")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /unban <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            plugin.getStaffManager().unban(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Vous avez unban " + ChatColor.AQUA + args[0] + ".");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <joueur> <raison...>");
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Sanction.Type type = label.equalsIgnoreCase("mute") ? Sanction.Type.MUTE : Sanction.Type.BAN;

        // Récupérer l'UUID de la cible
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            player.sendMessage(ChatColor.RED + "Joueur inconnu.");
            return true;
        }

        String id = plugin.getStaffManager().createPendingSanction(type, targetName, target.getUniqueId(), reason, player.getName());

        player.sendMessage(ChatColor.GREEN + "Demande de " + type.name().toLowerCase() + " pour " + ChatColor.AQUA + targetName + 
            ChatColor.GREEN + " envoyée pour validation (ID: " + ChatColor.WHITE + id + ChatColor.GREEN + ").");

        return true;
    }
}
