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
                                .addOption(OptionType.CHANNEL, "salon", "Le salon textuel pour les logs", true),
                        Commands.slash("acceptstaff", "Accepter une sanction staff")
                                .addOption(OptionType.STRING, "id", "L'ID de la sanction", true)
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
        } else if (event.getName().equals("acceptstaff")) {
            handleAcceptStaff(event);
        }
    }

    private void handleAcceptStaff(SlashCommandInteractionEvent event) {
        String id = event.getOption("id").getAsString();
        if (plugin.getStaffManager().acceptSanction(id)) {
            event.reply("La sanction **" + id + "** a été acceptée et appliquée.").queue();
        } else {
            event.reply("ID de sanction invalide ou déjà traitée.").setEphemeral(true).queue();
        }
    }

    public void sendStaffSanctionRequest(fr.rammex.chasetag.lobby.staff.Sanction sanction) {
        String channelId = plugin.getConfig().getString("discord.staff-validation-channel-id");
        if (channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚖️ Demande de Sanction Staff");
        embed.setColor(sanction.getType() == fr.rammex.chasetag.lobby.staff.Sanction.Type.BAN ? Color.RED : Color.ORANGE);
        embed.addField("Type", sanction.getType().name(), true);
        embed.addField("Cible", sanction.getTargetName(), true);
        embed.addField("Raison", sanction.getReason(), false);
        embed.addField("Staff", sanction.getStaffName(), true);
        embed.addField("ID de Validation", "`" + sanction.getId() + "`", true);
        embed.setFooter("Utilisez /acceptstaff " + sanction.getId() + " pour valider");
        embed.setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
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
        boolean whitelisted = action.equalsIgnoreCase("add");
        
        plugin.getPlayerMongoRepository().getPlayerByName(pseudo).ifPresentOrElse(player -> {
            player.setPlayerData("whitelisted", whitelisted);
            plugin.getPlayerMongoRepository().savePlayer(player);
            
            // Mettre à jour en cache si connecté
            Player cached = PlayerManager.getPlayer(player.getPlayerUUID());
            if (cached != null) cached.setPlayerData("whitelisted", whitelisted);

            event.reply("Le joueur **" + pseudo + "** a été " + (whitelisted ? "ajouté à" : "retiré de") + " la whitelist.").queue();
        }, () -> {
            if (whitelisted) {
                // Tenter de récupérer l'UUID via Mojang pour un nouveau joueur
                event.deferReply().queue();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        String uuid = fetchUUID(pseudo);
                        if (uuid != null) {
                            Player newPlayer = new Player(uuid, pseudo, Rank.Joueur);
                            newPlayer.setPlayerData("whitelisted", true);
                            plugin.getPlayerMongoRepository().savePlayer(newPlayer);
                            event.getHook().sendMessage("Le nouveau joueur **" + pseudo + "** a été ajouté à la whitelist (Profil créé).").queue();
                        } else {
                            // On ajoute quand même à la whitelist avec un UUID temporaire
                            String tempUuid = "UNKNOWN-" + pseudo;
                            Player newPlayer = new Player(tempUuid, pseudo, Rank.Joueur);
                            newPlayer.setPlayerData("whitelisted", true);
                            plugin.getPlayerMongoRepository().savePlayer(newPlayer);
                            event.getHook().sendMessage("Le joueur **" + pseudo + "** a été ajouté à la whitelist (UUID non trouvé, il sera synchronisé à sa connexion).").queue();
                        }
                    } catch (Exception e) {
                        event.getHook().sendMessage("Erreur lors de la récupération de l'UUID : " + e.getMessage()).queue();
                    }
                });
            } else {
                event.reply("Joueur non trouvé dans la base de données.").setEphemeral(true).queue();
            }
        });
    }

    private String fetchUUID(String playerName) throws Exception {
        java.net.URL url = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        if (connection.getResponseCode() == 200) {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Format simple car on ne veut que l'id: {"name":"PlayerName","id":"uuid"}
            String json = response.toString();
            int idIndex = json.indexOf("\"id\":\"") + 6;
            String id = json.substring(idIndex, json.indexOf("\"", idIndex));
            
            // Mojang renvoie l'UUID sans tirets, on doit les ajouter pour Bukkit
            return id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        }
        return null;
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
