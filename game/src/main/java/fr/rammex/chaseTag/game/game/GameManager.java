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
        game.setGameState(GameState.PLAYING);
        startRound();
    }

    public void startRound() {
        if (game == null || game.getGameState() != GameState.PLAYING) return;

        // Assigner les rôles pour la manche actuelle du round
        assignRoles();

        Player chaser = game.getCurrentChaser();
        Player runner = game.getCurrentRunner();

        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta meta = shears.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.setUnbreakable(true);
            shears.setItemMeta(meta);
        }

        // Téléporter et équiper les joueurs
        if (chaser != null && chaser.getBukkitPlayer() != null) {
            org.bukkit.entity.Player chaserBukkit = chaser.getBukkitPlayer();
            chaserBukkit.teleport(game.getArena().getRedSpawn());
            chaserBukkit.setHealth(20.0);
            chaserBukkit.setFoodLevel(20);
            chaserBukkit.getInventory().clear();
            chaserBukkit.getInventory().addItem(new ItemStack(Material.RED_WOOL, 64));
            chaserBukkit.getInventory().addItem(shears);
            chaserBukkit.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 1));
            chaserBukkit.sendMessage(ChatColor.RED + "Vous êtes le CHASSEUR ! Taguez le chassé !");
            }
            if (runner != null && runner.getBukkitPlayer() != null) {
            org.bukkit.entity.Player runnerBukkit = runner.getBukkitPlayer();
            runnerBukkit.teleport(game.getArena().getBlueSpawn());
            runnerBukkit.setHealth(1.0); // 1 HP = 0.5 coeur
            runnerBukkit.setFoodLevel(20);
            runnerBukkit.getInventory().clear();
            runnerBukkit.getInventory().addItem(new ItemStack(Material.BLUE_WOOL, 64));
            runnerBukkit.getInventory().addItem(shears);
            runnerBukkit.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 1));
            runnerBukkit.sendMessage(ChatColor.BLUE + "Vous êtes le CHASSÉ ! Fuyez ! (Vous avez 1 HP)");
            } else if (game.isTestDev() && runner == null) {
                // On est en test dev et il n'y a pas de runner (un seul joueur)
                // Spawn un cochon à la place
                if (testPig != null) testPig.remove();
                testPig = game.getArena().getBlueSpawn().getWorld().spawnEntity(game.getArena().getBlueSpawn(), EntityType.PIG);
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

        startCountdown();
    }

    private void startCountdown() {
        game.setCountdown(true);
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count > 0) {
                    String color = count > 3 ? "§a" : (count > 1 ? "§e" : "§c");
                    String title = color + count;
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(title, "§fPréparez-vous !", 0, 25, 0));
                    count--;
                } else {
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle("§6§lGO!", "", 0, 20, 10));
                    game.setCountdown(false);
                    
                    // Démarrer le chrono de 60 secondes (1 minute)
                    plugin.getTimerManager().createTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche(), 60)
                            .onFinished(() -> {
                                if (game.isTestDev() && game.getCurrentRunner() == null) {
                                    // Pas de gagnant ou le chassé gagne par défaut (le cochon)
                                    // Pour le test dev on peut juste dire que le temps est fini
                                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Temps écoulé !");
                                    endRound(game.getCurrentChaser(), 0); // On finit sans point ou autre
                                } else {
                                    endRound(game.getCurrentRunner(), 2);
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
            plugin.getTimerManager().removeTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
            endRound(chaser, 1);
        }
    }

    public void onPigTag(Player chaser) {
        if (game == null || game.getGameState() != GameState.PLAYING || game.isCountdown()) return;
        if (game.isTestDev() && chaser.equals(game.getCurrentChaser())) {
            plugin.getTimerManager().removeTimer("round_" + game.getCurrentRound() + "_" + game.getCurrentManche());
            endRound(chaser, 1);
        }
    }

    public void endRound(Player winner, int points) {
        if (game == null) return;

        if (testPig != null) {
            testPig.remove();
            testPig = null;
        }

        if (winner != null) {
            for (int i = 0; i < points; i++) {
                game.incrementScore(winner.getPlayerUUID());
            }
            
            String pointSuffix = points > 1 ? " points" : " point";
            Bukkit.broadcastMessage(ChatColor.GREEN + "La manche est terminée ! " + winner.getBukkitPlayer().getName() + " gagne " + points + pointSuffix);
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

        List<String> playerUuids = game.getPlayers().stream()
                .map(Player::getPlayerUUID)
                .collect(Collectors.toList());

        plugin.onGameFinished(winnerUuid, winnerName, playerUuids);
    }

    public Game getGame() {
        return game;
    }
}
