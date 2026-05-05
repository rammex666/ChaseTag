package fr.rammex.chasetag.lobby.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class Menu implements InventoryHolder {
    protected Player player;
    protected Inventory inventory;

    public Menu(Player player) {
        this.player = player;
    }

    public abstract void open();

    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {}

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
