package fr.rammex.chasetag.lobby.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public abstract class Menu implements InventoryHolder {
    protected Player player;

    public Menu(Player player) {
        this.player = player;
    }

    public abstract void open();
}
