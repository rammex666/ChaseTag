package fr.rammex.chasetag.lobby.menu;

import fr.rammex.chasetag.lobby.ChaseTagLobby;
import fr.rammex.chasetag.lobby.duel.DuelRequestManager;
import fr.rammex.chasetag.lobby.tournament.TournamentManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapSelectionMenu extends Menu {
    private final ChaseTagLobby plugin;
    private final DuelRequestManager duelRequestManager;
    private final TournamentManager tournamentManager;
    private final String targetName;
    private final boolean duelMode;
    private final boolean selfMode;
    private final String phase;
    private final int pool;
    private final int matchId;

    public MapSelectionMenu(Player player, ChaseTagLobby plugin, String targetName) {
        super(player);
        this.plugin = plugin;
        this.duelRequestManager = plugin.getDuelRequestManager();
        this.tournamentManager = null;
        this.targetName = targetName;
        this.duelMode = true;
        this.selfMode = false;
        this.phase = null;
        this.pool = -1;
        this.matchId = -1;
        this.inventory = player.getServer().createInventory(this, 27, "Choisir la map pour " + targetName);
    }

    public MapSelectionMenu(Player player, ChaseTagLobby plugin, String targetName, boolean selfMode) {
        super(player);
        this.plugin = plugin;
        this.duelRequestManager = plugin.getDuelRequestManager();
        this.tournamentManager = null;
        this.targetName = targetName;
        this.duelMode = true;
        this.selfMode = selfMode;
        this.phase = null;
        this.pool = -1;
        this.matchId = -1;
        this.inventory = player.getServer().createInventory(this, 27,
                "Choisir la map pour " + targetName);
    }

    public MapSelectionMenu(Player player, ChaseTagLobby plugin, TournamentManager tournamentManager,
                            String phase, int pool, int matchId) {
        super(player);
        this.plugin = plugin;
        this.duelRequestManager = null;
        this.tournamentManager = tournamentManager;
        this.targetName = null;
        this.duelMode = false;
        this.selfMode = false;
        this.phase = phase;
        this.pool = pool;
        this.matchId = matchId;
        this.inventory = player.getServer().createInventory(this, 27,
                "Choisir la map pour Match " + matchId);
    }

    @Override
    public void open() {
        setMenuItems();
        player.openInventory(inventory);
    }

    private void setMenuItems() {
        ItemStack background = MenuUtils.createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }

        List<Map<?, ?>> mapEntries = plugin.getConfig().getMapList("pterodactyl.maps");
        if (mapEntries == null || mapEntries.isEmpty()) {
            mapEntries = Collections.emptyList();
        }

        List<Map<?, ?>> allowedMaps = new ArrayList<>();
        for (Map<?, ?> entry : mapEntries) {
            if (isAllowedForCurrentMode(entry)) {
                allowedMaps.add(entry);
            }
        }

        int slot = 10;
        if (allowedMaps.isEmpty()) {
            inventory.setItem(13, MenuUtils.createMenuItem(
                    Material.BARRIER,
                    ChatColor.RED + "Aucune map disponible",
                    List.of(ChatColor.GRAY + "Aucune map compatible avec ce mode.")));
        }
        for (Map<?, ?> entry : allowedMaps) {
            if (slot >= 16) {
                break;
            }
            String name = getString(entry, "name", "Map inconnue");
            int eggId = getInt(entry, "egg-id", 0);
            String mapMode = getString(entry, "mode", "both");
            inventory.setItem(slot, MenuUtils.createMenuItem(
                    Material.PAPER,
                    ChatColor.AQUA + name,
                    List.of(
                            ChatColor.GRAY + "Egg ID: " + eggId,
                            ChatColor.GRAY + "Mode: " + mapMode,
                            ChatColor.GRAY + "Clic pour choisir cette map"
                    )
            ));
            slot++;
        }

        ItemStack randomItem = MenuUtils.createMenuItem(
                Material.EMERALD,
                ChatColor.GREEN + "Map aléatoire",
                List.of(ChatColor.GRAY + "Choisit une map au hasard")
        );
        inventory.setItem(16, randomItem);

        ItemStack back = MenuUtils.createBackButton(ChatColor.YELLOW + "Retour", ChatColor.GRAY + "Retour au menu précédent");
        inventory.setItem(26, back);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(ChatColor.YELLOW + "Retour")) {
            player.closeInventory();
            return;
        }

        if (displayName.equals(ChatColor.GREEN + "Map aléatoire")) {
            chooseRandomMap();
            return;
        }

        if (displayName.startsWith(ChatColor.AQUA.toString())) {
            String mapName = ChatColor.stripColor(displayName);
            int eggId = 0;
            if (item.getItemMeta().getLore() != null && !item.getItemMeta().getLore().isEmpty()) {
                String loreLine = item.getItemMeta().getLore().get(0);
                if (loreLine.contains("Egg ID: ")) {
                    try {
                        eggId = Integer.parseInt(loreLine.replace("Egg ID: ", ""));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            chooseMap(mapName, eggId);
        }
    }

    private void chooseRandomMap() {
        List<Map<?, ?>> mapEntries = plugin.getConfig().getMapList("pterodactyl.maps");
        if (mapEntries == null || mapEntries.isEmpty()) {
            chooseMap("Par défaut", 0);
            return;
        }

        List<Map<?, ?>> allowedMaps = new ArrayList<>();
        for (Map<?, ?> entry : mapEntries) {
            if (isAllowedForCurrentMode(entry)) {
                allowedMaps.add(entry);
            }
        }
        if (allowedMaps.isEmpty()) {
            chooseMap("Par défaut", 0);
            return;
        }

        Map<?, ?> randomEntry = allowedMaps.get(new Random().nextInt(allowedMaps.size()));
        String name = getString(randomEntry, "name", "Map inconnue");
        int eggId = getInt(randomEntry, "egg-id", 0);
        chooseMap(name, eggId);
    }

    private void chooseMap(String mapName, int eggId) {
        if (eggId <= 0) {
            eggId = plugin.getConfig().getInt("pterodactyl.egg-id", 0);
        }

        if (duelMode) {
            if (selfMode) {
                plugin.getGameManager().createSoloSession(player.getUniqueId(), eggId, mapName);
                player.sendMessage(ChatColor.GREEN + "Duel de test lancé ! Serveur en cours de démarrage...\n" + "§aMap: §e" + mapName);
                player.closeInventory();
            } else {
                duelRequestManager.createRequest(player.getName(), targetName, eggId, mapName);
                player.sendMessage(ChatColor.GREEN + "Demande de duel envoyée vers " + targetName + " avec la map " + mapName + " (egg " + eggId + ").");
                player.closeInventory();
            }
        } else {
            tournamentManager.setMatchMap(phase, pool, matchId, eggId, mapName);
            player.sendMessage(ChatColor.GREEN + "Map " + mapName + " sélectionnée pour le match " + matchId + ".");
            player.closeInventory();
        }
    }

    private boolean isAllowedForCurrentMode(Map<?, ?> entry) {
        String modeValue = getString(entry, "mode", "both");
        modeValue = modeValue.toLowerCase();
        return switch (modeValue) {
            case "practice", "duel" -> duelMode;
            case "tournament" -> !duelMode;
            default -> true;
        };
    }

    private String getString(Map<?, ?> entry, String key, String defaultValue) {
        Object value = entry.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int getInt(Map<?, ?> entry, String key, int defaultValue) {
        Object value = entry.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
