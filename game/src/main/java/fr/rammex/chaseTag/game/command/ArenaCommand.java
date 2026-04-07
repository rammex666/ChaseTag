package fr.rammex.chaseTag.game.command;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.arena.Arena;
import fr.rammex.chaseTag.game.arena.ArenaManager;
import fr.rammex.chaseTag.game.arena.creation.ArenaTool;
import fr.rammex.chaseTag.game.arena.creation.event.ArenaCreationEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ArenaCommand implements CommandExecutor {
    private final ArenaTool arenaTool = new ArenaTool();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "tool":
                player.getInventory().addItem(arenaTool.getArenaTool());
                player.sendMessage(ChatColor.GREEN + "Outil de création d'arène donné.");
                break;

            case "create":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena create <id> <name>");
                    return true;
                }
                handleCreate(player, args[1], args[2]);
                break;

            case "setspawn":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena setspawn <id> <blue|red|spec>");
                    return true;
                }
                handleSetSpawn(player, args[1], args[2]);
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena remove <id>");
                    return true;
                }
                ArenaManager.removeArena(args[1]);
                player.sendMessage(ChatColor.GREEN + "Arène supprimée.");
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String id, String name) {
        Location p1 = ArenaCreationEvent.getFirstPosition(player);
        Location p2 = ArenaCreationEvent.getSecondPosition(player);

        if (p1 == null || p2 == null) {
            player.sendMessage(ChatColor.RED + "Vous devez définir les deux points avec l'outil d'arène d'abord !");
            return;
        }

        Arena arena = new Arena(id, name, p1.getWorld().getName(), p1.getX(), p2.getX(), p1.getY(), p2.getY(), p1.getZ(), p2.getZ());
        ArenaManager.addArena(arena);
        player.sendMessage(ChatColor.GREEN + "Arène '" + name + "' créée avec succès ! N'oubliez pas de définir les points de spawn.");
    }

    private void handleSetSpawn(Player player, String id, String type) {
        Arena arena = ArenaManager.getArena(id);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arène introuvable.");
            return;
        }

        Location loc = player.getLocation();

        switch (type.toLowerCase()) {
            case "blue":
                arena.setBlueSpawn(loc);
                player.sendMessage(ChatColor.BLUE + "Spawn BLEU défini.");
                break;
            case "red":
                arena.setRedSpawn(loc);
                player.sendMessage(ChatColor.RED + "Spawn ROUGE défini.");
                break;
            case "spec":
                arena.setSpecSpawn(loc);
                player.sendMessage(ChatColor.GRAY + "Spawn SPECTATEUR défini.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Type de spawn invalide (blue|red|spec).");
                return;
        }

        ArenaManager.save();
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- Commandes d'Arène ---");
        player.sendMessage(ChatColor.YELLOW + "/arena tool - Obtenir l'outil de sélection");
        player.sendMessage(ChatColor.YELLOW + "/arena create <id> <name> - Créer l'arène à partir de la sélection");
        player.sendMessage(ChatColor.YELLOW + "/arena setspawn <id> <blue|red|spec> - Définir un point de spawn");
        player.sendMessage(ChatColor.YELLOW + "/arena remove <id> - Supprimer une arène");
    }
}
