package fr.rammex.chasetag.lobby.podium;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.player.Player;
import fr.rammex.chasetag.lobby.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PodiumManager {
    private final ChaseTagLobby plugin;
    private final List<Location> podiumLocations = new ArrayList<>();
    private Location hologramLocation;
    private final List<String> statsToDisplay = List.of("wins", "games_played", "losses");
    private int currentStatIndex = 0;

    private final List<Entity> spawnedEntities = new ArrayList<>();
    private TextDisplay hologram;
    private Interaction interaction;

    public PodiumManager(ChaseTagLobby plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    public void loadLocations() {
        podiumLocations.clear();
        if (plugin.getConfig().contains("podium.locations")) {
            for (String key : plugin.getConfig().getConfigurationSection("podium.locations").getKeys(false)) {
                podiumLocations.add(plugin.getConfig().getLocation("podium.locations." + key));
            }
        }
        hologramLocation = plugin.getConfig().getLocation("podium.hologram");
    }

    public void savePodiumLocation(int index, Location loc) {
        plugin.getConfig().set("podium.locations." + index, loc);
        plugin.saveConfig();
        loadLocations();
        refreshPodium();
    }

    public void saveHologramLocation(Location loc) {
        plugin.getConfig().set("podium.hologram", loc);
        plugin.saveConfig();
        loadLocations();
        refreshPodium();
    }

    public void refreshPodium() {
        clearEntities();
        if (podiumLocations.size() < 3 || hologramLocation == null) return;

        String currentStat = statsToDisplay.get(currentStatIndex);
        List<Player> topPlayers = plugin.getPlayerMongoRepository().getTopPlayers(currentStat, 3);

        spawnHologram(currentStat, topPlayers);

        for (int i = 0; i < 3; i++) {
            if (i < topPlayers.size()) {
                spawnNPC(podiumLocations.get(i), topPlayers.get(i), i + 1);
            }
        }
    }

    private void spawnHologram(String stat, List<Player> topPlayers) {
        hologram = (TextDisplay) hologramLocation.getWorld().spawnEntity(hologramLocation, EntityType.TEXT_DISPLAY);
        hologram.setText(ColorUtils.format("&6&lClassement: &e&l" + stat.toUpperCase().replace("_", " ") + "\n&7(Cliquez pour changer)"));
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setMetadata("podium_hologram", new FixedMetadataValue(plugin, true));
        spawnedEntities.add(hologram);

        interaction = (Interaction) hologramLocation.getWorld().spawnEntity(hologramLocation, EntityType.INTERACTION);
        interaction.setInteractionHeight(2);
        interaction.setInteractionWidth(2);
        interaction.setMetadata("podium_interaction", new FixedMetadataValue(plugin, true));
        spawnedEntities.add(interaction);
    }

    private void spawnNPC(Location loc, Player player, int rank) {
        ArmorStand npc = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        npc.setBasePlate(false);
        npc.setArms(true);
        npc.setCustomName(ColorUtils.format("&e#" + rank + " &7- &f" + player.getPlayerName()));
        npc.setCustomNameVisible(true);
        npc.setMetadata("podium_npc", new FixedMetadataValue(plugin, true));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(player.getPlayerUUID())));
        head.setItemMeta(meta);

        npc.getEquipment().setHelmet(head);
        npc.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        npc.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
        npc.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));

        spawnedEntities.add(npc);
    }

    public void cycleStat() {
        currentStatIndex = (currentStatIndex + 1) % statsToDisplay.size();
        refreshPodium();
    }

    public void clearEntities() {
        for (Entity entity : spawnedEntities) {
            if (entity.isValid()) entity.remove();
        }
        spawnedEntities.clear();
        
        // Safety cleanup by metadata
        if (hologramLocation != null) {
            hologramLocation.getWorld().getEntities().stream()
                .filter(e -> e.hasMetadata("podium_npc") || e.hasMetadata("podium_hologram") || e.hasMetadata("podium_interaction"))
                .forEach(Entity::remove);
        }
    }
}
