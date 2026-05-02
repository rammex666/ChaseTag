package fr.rammex.chasetag.lobby.discord;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.Player;
import fr.rammex.chasetag.lobby.player.PlayerManager;
import fr.rammex.chasetag.lobby.player.rank.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {

    private final ChaseTagLobby plugin;
    private JDA jda;
    private final String adminRoleId;

    public DiscordBot(ChaseTagLobby plugin) {
        this.plugin = plugin;
        this.adminRoleId = plugin.getConfig().getString("discord.admin-role-id");
        String token = plugin.getConfig().getString("discord.token");
        String guildId = plugin.getConfig().getString("discord.guild-id");

        if (token == null || token.isEmpty() || token.equals("YOUR_DISCORD_TOKEN")) {
            plugin.getLogger().warning("Discord token non configuré. Le bot ne démarrera pas.");
            return;
        }

        try {
            this.jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .build();
            this.jda.awaitReady();

            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(
                        Commands.slash("wl", "Gérer la whitelist")
                                .addOptions(new OptionData(OptionType.STRING, "action", "add ou remove").setRequired(true)
                                        .addChoice("add", "add")
                                        .addChoice("remove", "remove"))
                                .addOption(OptionType.STRING, "pseudo", "Pseudo Minecraft du joueur", true),
                        Commands.slash("setrank", "Modifier le grade d'un joueur")
                                .addOption(OptionType.STRING, "pseudo", "Pseudo Minecraft du joueur", true)
                                .addOption(OptionType.STRING, "rank", "Le nouveau grade", true, true),
                        Commands.slash("setlogchannel", "Définir le salon de logs des parties")
                                .addOption(OptionType.CHANNEL, "salon", "Le salon textuel pour les logs", true)
                ).queue();
            }

            plugin.getLogger().info("Discord Bot connecté et commandes enregistrées.");
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du démarrage du bot Discord : " + e.getMessage());
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || member.getRoles().stream().noneMatch(role -> role.getId().equals(adminRoleId))) {
            event.reply("Vous n'avez pas la permission d'utiliser cette commande.").setEphemeral(true).queue();
            return;
        }

        if (event.getName().equals("wl")) {
            String action = event.getOption("action").getAsString();
            String pseudo = event.getOption("pseudo").getAsString();
            handleWhitelist(event, action, pseudo);
        } else if (event.getName().equals("setrank")) {
            String pseudo = event.getOption("pseudo").getAsString();
            String rankName = event.getOption("rank").getAsString();
            handleSetRank(event, pseudo, rankName);
        } else if (event.getName().equals("setlogchannel")) {
            handleSetLogChannel(event);
        }
    }

    private void handleSetLogChannel(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getOption("salon").getAsChannel().asTextChannel();
        if (channel == null) {
            event.reply("Veuillez sélectionner un salon textuel valide.").setEphemeral(true).queue();
            return;
        }

        plugin.getConfig().set("discord.game-logs-channel-id", channel.getId());
        plugin.saveConfig();
        event.reply("Le salon de logs a été défini sur " + channel.getAsMention()).queue();
    }

    public void sendGameStartLog(String player1, String player2, String mapName) {
        String channelId = plugin.getConfig().getString("discord.game-logs-channel-id");
        if (channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎮 Nouvelle Partie Lancée");
        embed.setColor(Color.GREEN);
        embed.addField("Joueur 1", player1, true);
        embed.addField("Joueur 2", player2, true);
        embed.addField("Map", mapName, false);
        embed.setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private void handleWhitelist(SlashCommandInteractionEvent event, String action, String pseudo) {
        plugin.getPlayerMongoRepository().getPlayerByName(pseudo).ifPresentOrElse(player -> {
            boolean whitelisted = action.equalsIgnoreCase("add");
            player.setPlayerData("whitelisted", whitelisted);
            plugin.getPlayerMongoRepository().savePlayer(player);
            
            // Mettre à jour en cache si connecté
            Player cached = PlayerManager.getPlayer(player.getPlayerUUID());
            if (cached != null) cached.setPlayerData("whitelisted", whitelisted);

            event.reply("Le joueur **" + pseudo + "** a été " + (whitelisted ? "ajouté à" : "retiré de") + " la whitelist.").queue();
        }, () -> {
            if (action.equalsIgnoreCase("add")) {
                // Créer un profil temporaire ou demander au joueur de se connecter une fois ?
                // Idéalement, on a besoin de l'UUID. On va essayer de le fetch via Mojang si possible ou informer qu'il doit s'être connecté.
                event.reply("Joueur non trouvé dans la base de données. Il doit s'être connecté au moins une fois.").setEphemeral(true).queue();
            } else {
                event.reply("Joueur non trouvé.").setEphemeral(true).queue();
            }
        });
    }

    private void handleSetRank(SlashCommandInteractionEvent event, String pseudo, String rankName) {
        try {
            Rank rank = Rank.valueOf(rankName);
            plugin.getPlayerMongoRepository().getPlayerByName(pseudo).ifPresentOrElse(player -> {
                player.setPlayerRole(rank);
                plugin.getPlayerMongoRepository().savePlayer(player);

                Player cached = PlayerManager.getPlayer(player.getPlayerUUID());
                if (cached != null) cached.setPlayerRole(rank);

                event.reply("Le grade de **" + pseudo + "** a été mis à jour vers **" + rank.name() + "**.").queue();
                
                org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(pseudo);
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage("§aVotre grade a été mis à jour vers " + rank.getPrefix() + " §apar un administrateur Discord.");
                }
            }, () -> event.reply("Joueur non trouvé.").setEphemeral(true).queue());
        } catch (IllegalArgumentException e) {
            String availableRanks = Arrays.stream(Rank.values()).map(Enum::name).collect(Collectors.joining(", "));
            event.reply("Grade invalide. Grades disponibles : " + availableRanks).setEphemeral(true).queue();
        }
    }

    public void shutdown() {
        if (jda != null) jda.shutdown();
    }
}
