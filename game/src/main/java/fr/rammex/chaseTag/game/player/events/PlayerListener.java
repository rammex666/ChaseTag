package fr.rammex.chaseTag.game.player.events;

import fr.rammex.chaseTag.game.ChaseTag;
import fr.rammex.chaseTag.game.arena.Arena;
import fr.rammex.chaseTag.game.game.Game;
import fr.rammex.chaseTag.game.game.GameState;
import fr.rammex.chaseTag.game.player.Player;
import fr.rammex.chaseTag.game.player.PlayerManager;
import fr.rammex.chaseTag.game.player.Role;
import fr.rammex.chaseTag.game.game.SpectatorManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoinFirstTime(PlayerJoinEvent event){
        org.bukkit.entity.Player player = event.getPlayer();
        if(!player.hasPlayedBefore()){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Role.None);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
        }


        Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

        if(player1 == null){
            Player playerRegistry = new Player(player.getUniqueId().toString(), Role.None);
            PlayerManager.addPlayer(playerRegistry);
            PlayerManager.save();
            player1 = PlayerManager.getPlayer(player.getUniqueId().toString());
        }

            if (player.hasPermission("chasetag.staff")) {
                player.sendMessage("§b[Staff] §fVous avez rejoint en tant que membre du staff.");
                player.setGameMode(GameMode.SPECTATOR);
                Game currentGame = ChaseTag.getInstance().getGameManager().getGame();
                if (currentGame != null) {
                    currentGame.getSpectators().add(player1);
                    player.teleport(currentGame.getArena().getSpecSpawn());
                    SpectatorManager.giveSpectatorItems(player);
                }
                return;
            }

        Game currentGame = ChaseTag.getInstance().getGameManager().getGame();
        
        if (currentGame != null) {
            // Reconnexion d'un joueur à sa partie
            if (currentGame.getPlayers().contains(player1)) {
                player.setGameMode(GameMode.SURVIVAL);
                // On le téléporte au spawn approprié selon son rôle
                if (player1.getPlayerRole() == Role.Chase) {
                    player.teleport(currentGame.getArena().getRedSpawn());
                } else {
                    player.teleport(currentGame.getArena().getBlueSpawn());
                }
                
                ChaseTag.getInstance().getGameManager().resumeGame(player.getName());
                return;
            }

            // Spectateur si la partie est déjà lancée
            if (currentGame.getGameState() != GameState.WAITING || Bukkit.getOnlinePlayers().stream().filter(p -> !p.hasPermission("chasetag.staff")).count() > 2) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§7La partie est complète ou déjà lancée. Vous êtes en mode spectateur.");
                
                currentGame.getSpectators().add(player1);
                player1.setPlayerRole(Role.Spec);
                player.teleport(currentGame.getArena().getSpecSpawn());
                SpectatorManager.giveSpectatorItems(player);
                return;
            }
        }

        Bukkit.getScheduler().runTaskLater(ChaseTag.getInstance(), () -> {
        long nonStaffCount = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.hasPermission("chasetag.staff")).count();
        
        if (nonStaffCount >= 2 && ChaseTag.getInstance().getGameManager().getGame() == null) {
            Arena arena = ChaseTag.getInstance().getArenaManager().getAll().values()
                .stream().findFirst().orElse(null);
            if (arena == null) {
                player.sendMessage("§cAucune arène disponible !");
                return;
            }

            List<Player> activePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.hasPermission("chasetag.staff"))
                .limit(2)
                .map(p -> PlayerManager.getPlayer(p.getUniqueId().toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (activePlayers.size() < 2) return;

            Game game = new Game(ChaseTag.getInstance().getServerId(), arena);
            game.setPlayers(activePlayers);

            List<Player> spectators = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("chasetag.staff"))
                .map(p -> PlayerManager.getPlayer(p.getUniqueId().toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            game.setSpectators(spectators);

            ChaseTag.getInstance().getGameManager().initGame(game);
            
            Bukkit.getScheduler().runTaskLater(ChaseTag.getInstance(), () -> {
                ChaseTag.getInstance().getGameManager().startGame();
            }, 60L);
        }
    }, 5L); // 5 ticks = laisser le temps au joueur d'être enregistré   
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game != null && (game.isCountdown() || game.getGameState() == GameState.PAUSE)) {
            event.setCancelled(true);
            return;
        }

        org.bukkit.entity.Player player = event.getPlayer();
        Player gamePlayer = PlayerManager.getPlayer(player.getUniqueId().toString());
        if (gamePlayer == null) return;

        Material type = event.getBlock().getType();

        if (type == Material.RED_WOOL && gamePlayer.getPlayerRole() != Role.Chase) {
            event.setCancelled(true);
            return;
        }
        if (type == Material.BLUE_WOOL && gamePlayer.getPlayerRole() != Role.Run) {
            event.setCancelled(true);
            return;
        }

        if (type == Material.RED_WOOL || type == Material.BLUE_WOOL) {
            if (game != null && game.getArena() != null) {
                Arena arena = game.getArena();

                if (!arena.isInside(event.getBlock().getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage("§cVous ne pouvez pas poser de blocs en dehors de l'arène !");
                    return;
                }

                int maxHeight = arena.getMaxWoolTowerHeight();
                double minY = Math.min(arena.getY1(), arena.getY2());
                double currentHeight = event.getBlock().getY() - minY + 1;

                if (currentHeight > maxHeight) {
                    event.setCancelled(true);
                    player.sendMessage("§cVous ne pouvez pas construire une tour de laine de plus de " + maxHeight + " blocs de haut par rapport au sol de l'arène !");
                    return;
                }
            }
            ChaseTag.getInstance().getGameManager().addPlacedBlock(event.getBlock());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game == null) return;

        // Boussole spectateur
        if (player.getGameMode() == GameMode.SPECTATOR && 
            (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (player.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
                SpectatorManager.handleSpectatorInteract(player, game);
                return;
            }
        }

        if (game.isCountdown() || game.getGameState() == GameState.PAUSE) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedBlock() != null) {
            Material type = event.getClickedBlock().getType();
            String typeName = type.name();
            if (typeName.contains("TRAPDOOR") || typeName.contains("FENCE_GATE") || typeName.contains("DOOR")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game != null && event.getBlock().getType() == Material.CACTUS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game == null) return;
        
        if (game.isCountdown() || game.getGameState() == GameState.PAUSE) {
            event.setCancelled(true);
            return;
        }

        Material type = event.getBlock().getType();
        if (type == Material.RED_WOOL || type == Material.BLUE_WOOL) {
            event.setDropItems(false);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onTag(EntityDamageByEntityEvent event) {
        Game game = ChaseTag.getInstance().getGameManager().getGame();
        if (game != null && (game.isCountdown() || game.getGameState() == GameState.PAUSE)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getDamager() instanceof org.bukkit.entity.Player)) {
            return;
        }

        org.bukkit.entity.Player attacker = (org.bukkit.entity.Player) event.getDamager();
        Player damager = PlayerManager.getPlayer(attacker.getUniqueId().toString());

        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player victimBukkit = (org.bukkit.entity.Player) event.getEntity();
            Player victim = PlayerManager.getPlayer(victimBukkit.getUniqueId().toString());

            if (damager != null && victim != null) {
                if (damager.getPlayerRole() == Role.Chase && victim.getPlayerRole() == Role.Run) {
                    event.setCancelled(true);
                    attacker.playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
                    victimBukkit.playSound(victimBukkit.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
                    ChaseTag.getInstance().getGameManager().onTag(damager, victim);
                } 
                else if (damager.getPlayerRole() == Role.Run && victim.getPlayerRole() == Role.Chase) {
                    event.setCancelled(true);
                }
            }
        } else if (game != null && game.isTestDev()) {
            org.bukkit.entity.Entity testPig = ChaseTag.getInstance().getGameManager().getTestPig();
            if (testPig != null && event.getEntity().equals(testPig)) {
                event.setCancelled(true);
                if (damager != null && damager.getPlayerRole() == Role.Chase) {
                    attacker.playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
                    ChaseTag.getInstance().getGameManager().onPigTag(damager);
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

@EventHandler
public void onPlayerMessage(AsyncChatEvent event){
    org.bukkit.entity.Player player = event.getPlayer();
    Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());

    String message = PlainTextComponentSerializer.plainText().serialize(event.message());
    event.setCancelled(true);
    Role playerRole = player1.getPlayerRole();

    String newMessage = playerRole.getPrefix()+" "+player.getName()+" >> "+message;
    Bukkit.broadcast(Component.text(newMessage));
}

@EventHandler
public void onQuit(PlayerQuitEvent event) {
    org.bukkit.entity.Player player = event.getPlayer();
    ChaseTag.getInstance().getScoreboardManager().removePlayer(player);
    
    Game game = ChaseTag.getInstance().getGameManager().getGame();
    if (game == null) return;
    
    Player player1 = PlayerManager.getPlayer(player.getUniqueId().toString());
    if (player1 == null) return;
    
    if (game.getPlayers().contains(player1)) {
        ChaseTag.getInstance().getGameManager().pauseGame(player.getName());
    } else if (game.getSpectators().contains(player1)) {
        game.getSpectators().remove(player1);
    }
}
}
