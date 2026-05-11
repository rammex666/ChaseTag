package fr.rammex.chaseTag.game.game;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.rammex.chaseTag.game.timer.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GameManager {
    private final ChaseTag plugin;
    private Game game;
    private final List<Block> placedBlocks = new ArrayList<>();
    private Entity testPig;

    public GameManager(ChaseTag plugin) {
        this.plugin = plugin;
    }

    public void initGame(Game game) {
        this.game = game;
    }

    public void startGame() {
        if (game == null) return;
        cleanupEntities();
        game.setGameState(GameState.PLAYING);
        
        org.bukkit.World world = game.getArena().getRedSpawn().getWorld();
        if (world != null) {
            world.setTime(1000);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        startRound();
    }

    public void startRound() {
        if (game == null || game.getGameState() != GameState.PLAYING) return;

        game.setCountdown(true);
        // Assigner les rôles pour la manche actuelle du round
        assignRoles();

        Player chaser = game.getCurrentChaser();
        Player runner = game.getCurrentRunner();

        // Rotation des spawns chaque round
        org.bukkit.Location redSpawn = game.getArena().getRedSpawn();
        org.bukkit.Location blueSpawn = game.getArena().getBlueSpawn();

        if (game.getCurrentRound() % 2 == 0) {
            org.bukkit.Location temp = redSpawn;
            redSpawn = blueSpawn;
            blueSpawn = temp;
        }

        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta meta = shears.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
            meta.setUnbreakable(true);
            shears.setItemMeta(meta);
        }

        ItemStack cobweb = new ItemStack(Material.COBWEB, 1);
        ItemMeta cobwebMeta = cobweb.getItemMeta();
        if (cobwebMeta != null) {
            cobwebMeta.setDisplayName(ChatColor.WHITE + "Toile de capture");
            cobweb.setItemMeta(cobwebMeta);
        }

        // Téléporter et équiper les joueurs
        if (chaser != null && chaser.getBukkitPlayer() != null) {
            org.bukkit.entity.Player chaserBukkit = chaser.getBukkitPlayer();
            chaserBukkit.teleport(redSpawn);
            chaserBukkit.setHealth(20.0);
            chaserBukkit.setFoodLevel(20);
            chaserBukkit.getInventory().clear();
            chaserBukkit.getInventory().addItem(new ItemStack(Material.RED_WOOL,  20));
            chaserBukkit.getInventory().addItem(shears);
            chaserBukkit.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 1));
            chaserBukkit.sendMessage(ChatColor.RED + "Vous êtes le CHASSEUR ! Taguez le chassé !");
            chaserBukkit.sendTitle(ChatColor.RED + "CHASSEUR", ChatColor.YELLOW + "Taguez le chassé !", 0, 40, 0);

            org.bukkit.attribute.AttributeInstance reach = chaserBukkit.getAttribute(org.bukkit.attribute.Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (reach != null) reach.setBaseValue(2.0);
        }
        if (runner != null && runner.getBukkitPlayer() != null) {
            org.bukkit.entity.Player runnerBukkit = runner.getBukkitPlayer();
            runnerBukkit.teleport(blueSpawn);
            runnerBukkit.setHealth(20.0);
            runnerBukkit.setFoodLevel(20);
            runnerBukkit.getInventory().clear();
            runnerBukkit.getInventory().addItem(new ItemStack(Material.BLUE_WOOL, 20));
            runnerBukkit.getInventory().addItem(shears);
            runnerBukkit.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 1));
            runnerBukkit.getInventory().addItem(cobweb);
            runnerBukkit.sendMessage(ChatColor.BLUE + "Vous êtes le CHASSÉ ! Fuyez !");
            runnerBukkit.sendTitle(ChatColor.BLUE + "CHASSÉ", ChatColor.YELLOW + "Fuyez !", 0, 40, 0);

            org.bukkit.attribute.AttributeInstance reach = runnerBukkit.getAttribute(org.bukkit.attribute.Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (reach != null) reach.setBaseValue(2.0);
        }
 else if (game.isTestDev() && runner == null) {
                if (testPig != null) testPig.remove();
                testPig = blueSpawn.getWorld().spawnEntity(blueSpawn, EntityType.PIG);
                Pig pig = (Pig) testPig;
                pig.setAI(false);
                pig.setInvulnerable(false); // Doit pouvoir recevoir un coup
                pig.setCustomName(ChatColor.BLUE + "Adversaire de Test");
                pig.setCustomNameVisible(true);
            }

        // Gérer les spectateurs
        for (Player spec : game.getSpectators()) {
            org.bukkit.entity.Player specBukkit = spec.getBukkitPlayer();
            if (specBukkit != null) {
                specBukkit.setGameMode(org.bukkit.GameMode.SPECTATOR);
                specBukkit.teleport(game.getArena().getSpecSpawn());
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::startCountdown, 20L);
    }

    private void startCountdown() {
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = 10;

            @Override
            public void run() {
                if (count > 0) {
                    String color = count > 6 ? "§a" : (count > 3 ? "§e" : "§c");
                    String title = color + count;
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.sendTitle(title, "§fPréparez-vous !", 0, 25, 0);
                        if (count <= 3) {
                            float pitch = (count == 1) ? 2.0f : 1.0f;
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                        }
                    });
                    count--;
                } else {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.sendTitle("§6§lGO!", "", 0, 20, 10);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    });
                    game.setCountdown(false);
                    
                    // Démarrer le chrono de 60 secondes (1 minute)
                    plugin.getTimerManager().createTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche(), 60)
                            .onFinished(() -> {
                                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f));
                                if (game.isTestDev() && game.getCurrentRunner() == null) {
                                    // Pas de gagnant ou le chassé gagne par défaut (le cochon)
                                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Temps écoulé !");
                                    endRound(60, 0); 
                                } else {
                                    endRound(60, 0);
                                }
                            })
                            .start();
                    
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Début du Round " + game.getCurrentRound() + " (Manche " + game.getCurrentManche() + "/2)");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void assignRoles() {
        List<Player> players = game.getPlayers();
        if (players.isEmpty()) return;

        if (game.isTestDev() && players.size() == 1) {
            game.setCurrentChaser(players.get(0));
            game.setCurrentRunner(null);
            game.getCurrentChaser().setPlayerRole(Role.Chase);
            return;
        }

        if (players.size() < 2) return;

        if (game.getCurrentChaser() == null) {
            List<Player> shuffle = new ArrayList<>(players);
            Collections.shuffle(shuffle);
            game.setCurrentChaser(shuffle.get(0));
            game.setCurrentRunner(shuffle.get(1));
        } else {
            Player oldChaser = game.getCurrentChaser();
            Player oldRunner = game.getCurrentRunner();
            game.setCurrentChaser(oldRunner);
            game.setCurrentRunner(oldChaser);
        }

        game.getCurrentChaser().setPlayerRole(Role.Chase);
        game.getCurrentRunner().setPlayerRole(Role.Run);
    }

    public void onTag(Player chaser, Player runner) {
        if (game == null || game.getGameState() != GameState.PLAYING || game.isCountdown()) return;
        
        if (chaser.equals(game.getCurrentChaser()) && runner.equals(game.getCurrentRunner())) {
            Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
            int elapsed = 60;
            if (timer != null) {
                elapsed = (int) timer.getElapsedSeconds();
                plugin.getTimerManager().removeTimer(timer.getId());
            }
            endRound(elapsed, 60 - elapsed);
        }
    }

    public void onPigTag(Player chaser) {
        if (game == null || game.getGameState() != GameState.PLAYING || game.isCountdown()) return;
        if (game.isTestDev() && chaser.equals(game.getCurrentChaser())) {
            Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
            int elapsed = 60;
            if (timer != null) {
                elapsed = (int) timer.getElapsedSeconds();
                plugin.getTimerManager().removeTimer(timer.getId());
            }
            endRound(elapsed, 60 - elapsed);
        }
    }

    public void endRound(int runnerPoints, int chaserPoints) {
        if (game == null) return;

        if (testPig != null) {
            testPig.remove();
            testPig = null;
        }

        Player runner = game.getCurrentRunner();
        Player chaser = game.getCurrentChaser();

        // Clear inventories to prevent item duplication
        for (Player p : game.getPlayers()) {
            if (p.getBukkitPlayer() != null) {
                p.getBukkitPlayer().getInventory().clear();
                p.getBukkitPlayer().setItemOnCursor(null);
            }
        }

        if (runner != null) {
            game.addScore(runner.getPlayerUUID(), runnerPoints);
        }
        if (chaser != null) {
            game.addScore(chaser.getPlayerUUID(), chaserPoints);
        }

        String runnerName = (runner != null && runner.getBukkitPlayer() != null) ? runner.getBukkitPlayer().getName() : "Le chassé";
        String chaserName = (chaser != null && chaser.getBukkitPlayer() != null) ? chaser.getBukkitPlayer().getName() : (game.isTestDev() ? "Le chasseur" : "Le chasseur");

        Bukkit.broadcastMessage(ChatColor.GREEN + "La manche est terminée !");
        if (runner != null || game.isTestDev()) {
            Bukkit.broadcastMessage(ChatColor.BLUE + runnerName + " gagne " + runnerPoints + " points (survie).");
        }
        if (chaser != null) {
            Bukkit.broadcastMessage(ChatColor.RED + chaserName + " gagne " + chaserPoints + " points (capture).");
        }

        clearPlacedBlocks();

        if (game.getCurrentManche() < game.getMaxManchesPerRound()) {
            game.setCurrentManche(game.getCurrentManche() + 1);
            startRound();
        } else {
            handleEndOfRound();
        }
    }

    private void handleEndOfRound() {
        if (game.isTestDev() && game.getPlayers().size() == 1) {
            if (game.getCurrentRound() >= game.getMaxRounds()) {
                endGame();
            } else {
                game.setCurrentRound(game.getCurrentRound() + 1);
                game.setCurrentManche(1);
                startRound();
            }
            return;
        }

        if (game.getPlayers().size() < 2) return;

        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        int s1 = game.getScore(p1.getPlayerUUID());
        int s2 = game.getScore(p2.getPlayerUUID());

        if (game.getCurrentRound() >= game.getMaxRounds()) {
            int diff = Math.abs(s1 - s2);
            if (s1 == s2 || diff < 2) {
                game.setCurrentRound(game.getCurrentRound() + 1);
                game.setCurrentManche(1);
                Bukkit.broadcastMessage(ChatColor.GOLD + "--- PROLONGATIONS (Round " + game.getCurrentRound() + ") ---");
                startRound();
            } else {
                endGame();
            }
        } else {
            game.setCurrentRound(game.getCurrentRound() + 1);
            game.setCurrentManche(1);
            startRound();
        }
    }

    public void pauseGame(String leaverName) {
        if (game == null || game.getGameState() != GameState.PLAYING) return;
        
        game.setGameState(GameState.PAUSE);
        Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
        if (timer != null) {
            timer.pause();
        }
        
        Bukkit.broadcastMessage(ChatColor.RED + "La partie est en PAUSE car " + ChatColor.YELLOW + leaverName + ChatColor.RED + " a quitté !");
    }

    public void resumeGame(String joinerName) {
        if (game == null || game.getGameState() != GameState.PAUSE) return;
        
        game.setGameState(GameState.PLAYING);
        Timer timer = plugin.getTimerManager().getTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
        if (timer != null) {
            timer.resume();
        }
        
        Bukkit.broadcastMessage(ChatColor.GREEN + "La partie REPREND car " + ChatColor.YELLOW + joinerName + ChatColor.GREEN + " est revenu !");
    }

    public Entity getTestPig() {
        return testPig;
    }

    private void clearPlacedBlocks() {
        for (Block block : placedBlocks) {
            block.setType(Material.AIR);
        }
        placedBlocks.clear();
    }

    public void addPlacedBlock(Block block) {
        placedBlocks.add(block);
    }

    public void forceEndGame(Player leaver) {
        if (game == null) return;
        
        Player winner = game.getPlayers().stream()
                .filter(p -> !p.equals(leaver))
                .findFirst()
                .orElse(null);

        String winnerName = "Aucun";
        String winnerUuid = "none";
        
        if (winner != null) {
            winnerUuid = winner.getPlayerUUID();
            if (winner.getBukkitPlayer() != null) {
                winnerName = winner.getBukkitPlayer().getName();
            } else {
                winnerName = winnerUuid;
            }
            // On lui donne un score symbolique pour la victoire par abandon
            game.addScore(winnerUuid, 1);
        }

        Bukkit.broadcastMessage(ChatColor.RED + leaver.getBukkitPlayer().getName() + " a abandonné la partie !");
        endGame();
    }

    private void endGame() {
        game.setGameState(GameState.END);
        
        Player winner = game.getPlayers().stream()
                .max((p1, p2) -> Integer.compare(game.getScore(p1.getPlayerUUID()), game.getScore(p2.getPlayerUUID())))
                .orElse(null);

        String winnerName = "Aucun";
        String winnerUuid = "none";
        
        if (winner != null) {
            winnerUuid = winner.getPlayerUUID();
            if (winner.getBukkitPlayer() != null) {
                winnerName = winner.getBukkitPlayer().getName();
            } else {
                winnerName = winnerUuid;
            }
        }
        
        Bukkit.broadcastMessage(ChatColor.AQUA + "La partie est terminée ! Vainqueur final : " + winnerName);
        Bukkit.broadcastMessage(ChatColor.GOLD + "--- Scores finaux ---");
        for (Player p : game.getPlayers()) {
            if (p.getBukkitPlayer() != null) {
                Bukkit.broadcastMessage(ChatColor.WHITE + p.getBukkitPlayer().getName() + ": " + ChatColor.YELLOW + game.getScore(p.getPlayerUUID()) + " points");
            }
        }

        List<String> playerUuids = game.getPlayers().stream()
                .map(Player::getPlayerUUID)
                .collect(Collectors.toList());

        plugin.onGameFinished(winnerUuid, winnerName, playerUuids, game.getPlayerScores());
    }

    public Game getGame() {
        return game;
    }

    private void cleanupEntities() {
        if (game == null || game.getArena() == null) return;
        org.bukkit.World world = game.getArena().getRedSpawn().getWorld();
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (entity.getType() == EntityType.PIG) {
                if (entity.getCustomName() != null && entity.getCustomName().contains("Adversaire de Test")) {
                    entity.remove();
                } else if (game.getArena().isInside(entity.getLocation())) {
                    // Supprimer tout cochon dans l'arène par sécurité
                    entity.remove();
                }
            }
        }
    }
}
