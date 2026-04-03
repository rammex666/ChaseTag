package fr.rammex.chaseTag.arena.creation;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArenaTool {

    public ItemStack getArenaTool(){
        ItemStack arenaTool = new ItemStack(Material.STICK, 1);
        ItemMeta itemMeta = arenaTool.getItemMeta();

        List<String> lore = new ArrayList<>();

        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',"&6Arena Tool"));


        lore.add(ChatColor.translateAlternateColorCodes('&',"&eClique gauche définir le premier point"));
        lore.add(ChatColor.translateAlternateColorCodes('&',"&6Clique droit définir le deuxième point"));

        itemMeta.setLore(lore);

        arenaTool.setItemMeta(itemMeta);

        return arenaTool;
    }
}
