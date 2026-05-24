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
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

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
                                .addOption(OptionType.STRING, "id", "L'ID de la sanction", true),
                        Commands.slash("participants", "Obtenir la liste des participants au tournoi"),
                        Commands.slash("stats", "Afficher les statistiques d'un joueur")
                                .addOption(OptionType.STRING, "pseudo", "Pseudo Minecraft du joueur", true),
                        Commands.slash("wllist", "Afficher la liste des joueurs whitelists")
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
        } else if (event.getName().equals("participants")) {
            handleParticipants(event);
        } else if (event.getName().equals("stats")) {
            String pseudo = event.getOption("pseudo").getAsString();
            handleStats(event, pseudo);
        } else if (event.getName().equals("wllist")) {
            handleWlList(event);
        }
    }

    private void handleWlList(SlashCommandInteractionEvent event) {
        java.util.List<Player> whitelisted = plugin.getPlayerMongoRepository().getWhitelistedPlayers();
        
        String list = whitelisted.stream()
                .map(Player::getPlayerName)
                .collect(Collectors.joining(", "));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📄 Liste de la Whitelist");
        embed.setColor(Color.WHITE);
        embed.setDescription(list.isEmpty() ? "Aucun joueur n'est whiteliste." : list);
        embed.setFooter("Total : " + whitelisted.size() + " joueurs");
        embed.setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleStats(SlashCommandInteractionEvent event, String pseudo) {
        plugin.getPlayerMongoRepository().getPlayerByName(pseudo).ifPresentOrElse(player -> {
            event.deferReply().queue();
            
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    byte[] imageBytes = generateStatsImage(player);
                    event.getHook().sendFiles(FileUpload.fromData(imageBytes, "stats_" + pseudo + ".png")).queue();
                } catch (Exception e) {
                    plugin.getLogger().severe("Erreur lors de la génération de l'image de stats : " + e.getMessage());
                    event.getHook().sendMessage("Erreur lors de la génération de l'image : " + e.getMessage()).queue();
                }
            });
        }, () -> event.reply("Joueur non trouvé dans la base de données.").setEphemeral(true).queue());
    }

    private byte[] generateStatsImage(Player player) throws IOException {
    final int WIDTH = 1200;
    final int HEIGHT = 600;

    // --- Fond ---
    File bgFile = new File(plugin.getDataFolder(), "assets/stats_bg.png");
    BufferedImage background;
    if (bgFile.exists()) {
        background = ImageIO.read(bgFile);
    } else {
        background = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = background.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Fond sombre dégradé diagonal
        GradientPaint gp = new GradientPaint(0, 0, new Color(12, 12, 18), WIDTH, HEIGHT, new Color(30, 28, 45));
        bg.setPaint(gp);
        bg.fillRect(0, 0, WIDTH, HEIGHT);
        bg.dispose();
    }

    BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    // Fond
    g.drawImage(background, 0, 0, WIDTH, HEIGHT, null);

    // Overlay sombre semi-transparent pour lisibilité
    g.setColor(new Color(0, 0, 0, 80));
    g.fillRect(0, 0, WIDTH, HEIGHT);

    // Séparateur vertical central
    g.setColor(new Color(255, 255, 255, 20));
    g.setStroke(new BasicStroke(1.5f));
    g.drawLine(WIDTH / 2 - 20, 40, WIDTH / 2 - 20, HEIGHT - 40);

    // ===================== CÔTÉ GAUCHE : SKIN =====================
    Rank rank = player.getPlayerRole();
    Color rankColor = Color.decode(rank.getHex1() != null ? rank.getHex1() : "#FFD700");

    int skinHeight = (int)(HEIGHT * 0.72);
    int skinX = 60;
    int skinY = HEIGHT - skinHeight - 30;

    // Largeur fixe utilisée pour centrer le texte même sans skin
    final int SKIN_WIDTH = 120; // largeur approximative du rendu mc-heads

    try {
        // Minotar - fonctionne avec le pseudo directement
        URLConnection conn = new URL("https://minotar.net/body/" + player.getPlayerName() + "/" + skinHeight).openConnection();
        conn.setRequestProperty("User-Agent", "ChaseTagPlugin/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        BufferedImage skin = ImageIO.read(conn.getInputStream());
        if (skin != null) {
            // Ombre portée sous le skin
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(skinX + 10, HEIGHT - 45, skin.getWidth() - 20, 30);
            g.drawImage(skin, skinX, skinY, null);
        }
    } catch (Exception e) {
        plugin.getLogger().warning("Skin introuvable pour " + player.getPlayerName() + ": " + e.getMessage());
    }

    // Pseudo + grade TOUJOURS dessinés, même si le skin a échoué
    {
        String rankLabel = rank.getPrefix().toUpperCase();
        g.setFont(loadFont("Segoe UI", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        int badgeW = fm.stringWidth(rankLabel) + 24;
        int badgeH = 30;
        int badgeX = skinX + (SKIN_WIDTH - badgeW) / 2;
        int badgeY = skinY - badgeH - 12;

        // Fond badge couleur du rang
        g.setColor(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 220));
        g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
        // Texte badge blanc
        g.setColor(Color.WHITE);
        g.drawString(rankLabel, badgeX + 12, badgeY + 21);

        // Pseudo
        g.setFont(loadFont("Segoe UI", Font.BOLD, 28));
        fm = g.getFontMetrics();
        String name = player.getPlayerName();
        int nameX = skinX + (SKIN_WIDTH - fm.stringWidth(name)) / 2;
        // Ombre
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(name, nameX + 2, badgeY - 6);
        // Couleur du rang pour le pseudo
        g.setColor(rankColor);
        g.drawString(name, nameX, badgeY - 8);
    }

    // ===================== CÔTÉ DROIT : STATS =====================
    int sx = WIDTH / 2;        // X de départ des stats
    int sy = 55;               // Y de départ

    // --- Titre OWNED CUP ---
    g.setFont(loadFont("Segoe UI", Font.BOLD, 58));
    String title = "OWNED CUP";
    FontMetrics titleFm = g.getFontMetrics();
    int titleX = sx + (WIDTH / 2 - titleFm.stringWidth(title)) / 2;
    // Ombre
    g.setColor(new Color(0, 0, 0, 120));
    g.drawString(title, titleX + 3, sy + 3);
    // Or avec léger dégradé simulé
    g.setColor(new Color(255, 215, 0));
    g.drawString(title, titleX, sy);

    // Ligne décorative sous le titre
    int lineY = sy + 18;
    GradientPaint lineGrad = new GradientPaint(sx, lineY, new Color(255, 215, 0, 0),
            sx + (WIDTH - sx) / 2, lineY, new Color(255, 215, 0, 200),
            true);
    g.setPaint(lineGrad);
    g.setStroke(new BasicStroke(2f));
    g.drawLine(sx + 20, lineY, WIDTH - 40, lineY);
    g.setStroke(new BasicStroke(1f));

    sy += 70;

    // --- Section STATISTIQUES ---
    drawSectionTitle(g, "STATISTIQUES", sx + 20, sy, rankColor);
    sy += 50;

    int wins   = player.getWins();
    int losses = player.getLosses();
    int games  = player.getGamesPlayed();

    drawBigStatCard(g, "Victoires",  String.valueOf(wins),   sx + 20,       sy, 180, new Color(46, 204, 113));
    drawBigStatCard(g, "Défaites",   String.valueOf(losses), sx + 220,      sy, 180, new Color(231, 76, 60));
    drawBigStatCard(g, "Parties",    String.valueOf(games),  sx + 420,      sy, 180, new Color(52, 152, 219));

    sy += 130;

    // --- Barre Win Rate ---
    float winRate = (games > 0) ? (wins * 100f / games) : 0f;
    drawWinRateBar(g, winRate, sx + 20, sy, WIDTH - sx - 60, rankColor);
    sy += 70;

    // --- Section RECORDS ---
    drawSectionTitle(g, "RECORDS PERSONNELS", sx + 20, sy, rankColor);
    sy += 45;

    String hunterTime = (player.getBestHunterTime() == 0) ? "N/A" : player.getBestHunterTime() + "s";
    String runnerTime = (player.getBestRunnerTime() == 0) ? "N/A" : player.getBestRunnerTime() + "s";

    drawRecordCard(g, "🏹 Chasseur", hunterTime, sx + 20,  sy, new Color(241, 196, 15));
    drawRecordCard(g, "🏃 Chassé",   runnerTime, sx + 310, sy, new Color(230, 126, 34));

    g.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "png", baos);
    return baos.toByteArray();
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Charge une police système avec fallback. */
private Font loadFont(String name, int style, int size) {
    return new Font(name, style, size);
}

/** Titre de section avec soulignement coloré. */
private void drawSectionTitle(Graphics2D g, String text, int x, int y, Color accentColor) {
    g.setFont(loadFont("Segoe UI", Font.BOLD, 22));
    g.setColor(new Color(200, 200, 200, 200));
    g.drawString(text, x, y);
    int w = g.getFontMetrics().stringWidth(text);
    g.setColor(accentColor);
    g.setStroke(new BasicStroke(2.5f));
    g.drawLine(x, y + 6, x + w, y + 6);
    g.setStroke(new BasicStroke(1f));
}

/**
 * Carte stat carrée avec grande valeur.
 * w = largeur de la carte.
 */
private void drawBigStatCard(Graphics2D g, String label, String value, int x, int y, int w, Color color) {
    int h = 100;
    int arc = 14;

    // Fond carte
    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
    g.fillRoundRect(x, y, w, h, arc, arc);

    // Bordure colorée (haut)
    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
    g.setStroke(new BasicStroke(2f));
    g.drawLine(x + arc / 2, y + 1, x + w - arc / 2, y + 1);
    g.setStroke(new BasicStroke(1f));

    // Valeur (grand)
    g.setFont(loadFont("Segoe UI", Font.BOLD, 46));
    FontMetrics fm = g.getFontMetrics();
    int vx = x + (w - fm.stringWidth(value)) / 2;
    // Ombre
    g.setColor(new Color(0, 0, 0, 100));
    g.drawString(value, vx + 2, y + 62);
    g.setColor(Color.WHITE);
    g.drawString(value, vx, y + 60);

    // Label (petit, en bas)
    g.setFont(loadFont("Segoe UI", Font.PLAIN, 15));
    fm = g.getFontMetrics();
    g.setColor(new Color(200, 200, 200, 180));
    g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + 90);
}

/**
 * Barre de progression Win Rate.
 */
private void drawWinRateBar(Graphics2D g, float winRate, int x, int y, int w, Color rankColor) {
    int h = 36;
    int arc = 18;

    // Label
    g.setFont(loadFont("Segoe UI", Font.BOLD, 16));
    g.setColor(new Color(200, 200, 200, 200));
    String label = String.format("WIN RATE  %.1f%%", winRate);
    g.drawString(label, x, y - 8);

    // Fond de la barre
    g.setColor(new Color(255, 255, 255, 25));
    g.fillRoundRect(x, y, w, h, arc, arc);

    // Remplissage
    int filled = Math.max(arc, (int)(w * winRate / 100f));
    // Dégradé vert → couleur rang
    GradientPaint gp = new GradientPaint(x, y, new Color(46, 204, 113),
            x + filled, y, rankColor);
    g.setPaint(gp);
    g.fillRoundRect(x, y, filled, h, arc, arc);

    // Bordure
    g.setColor(new Color(255, 255, 255, 40));
    g.setStroke(new BasicStroke(1.5f));
    g.drawRoundRect(x, y, w, h, arc, arc);
    g.setStroke(new BasicStroke(1f));

    // Pourcentage centré dans la barre
    g.setFont(loadFont("Segoe UI", Font.BOLD, 18));
    g.setColor(Color.WHITE);
    String pct = String.format("%.1f%%", winRate);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(pct, x + (w - fm.stringWidth(pct)) / 2, y + h / 2 + 7);
}

/**
 * Carte record (Chasseur / Chassé) avec valeur mise en valeur.
 */
private void drawRecordCard(Graphics2D g, String label, String value, int x, int y, Color color) {
    int w = 260, h = 90, arc = 12;

    // Fond
    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
    g.fillRoundRect(x, y, w, h, arc, arc);

    // Bordure gauche colorée
    g.setColor(color);
    g.setStroke(new BasicStroke(4f));
    g.drawLine(x + 2, y + arc / 2, x + 2, y + h - arc / 2);
    g.setStroke(new BasicStroke(1f));

    // Label
    g.setFont(loadFont("Segoe UI", Font.BOLD, 16));
    g.setColor(new Color(200, 200, 200, 190));
    g.drawString(label, x + 16, y + 28);

    // Valeur
    g.setFont(loadFont("Segoe UI", Font.BOLD, 38));
    g.setColor(color);
    g.drawString(value, x + 16, y + 72);
}

    private void handleParticipants(SlashCommandInteractionEvent event) {
        java.util.List<String> participants = plugin.getTournamentManager().getRemainingPlayers();
        java.util.List<String> eliminated = plugin.getTournamentManager().getEliminatedPlayers();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Participants au Tournoi");
        embed.setColor(Color.CYAN);
        embed.addField("En lice (" + participants.size() + ")", participants.isEmpty() ? "Aucun" : String.join(", ", participants), false);
        embed.addField("Éliminés (" + eliminated.size() + ")", eliminated.isEmpty() ? "Aucun" : String.join(", ", eliminated), false);
        embed.setFooter("Phase actuelle : " + plugin.getTournamentManager().getCurrentPhase());
        embed.setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
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
