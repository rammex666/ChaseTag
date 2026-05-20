package fr.rammex.chasetag.lobby.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public class ColorUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Crée un texte en gradient entre deux couleurs HEX.
     *
     * @param text      Le texte à colorer.
     * @param startHex  La couleur HEX de départ (ex: #FF0000).
     * @param endHex    La couleur HEX de fin (ex: #00FF00).
     * @return Le Component avec le gradient appliqué.
     */
    public static Component gradient(String text, String startHex, String endHex) {
        String mmString = String.format("<gradient:%s:%s>%s</gradient>", startHex, endHex, text);
        return miniMessage.deserialize(mmString);
    }

    /**
     * Crée un texte en gradient avec plusieurs couleurs HEX.
     *
     * @param text     Le texte à colorer.
     * @param hexCodes Liste des codes HEX (ex: #FF0000, #00FF00, #0000FF).
     * @return Le Component avec le gradient appliqué.
     */
    public static Component gradient(String text, String... hexCodes) {
        if (hexCodes == null || hexCodes.length == 0) {
            return Component.text(text);
        }
        
        StringBuilder colors = new StringBuilder();
        for (String hex : hexCodes) {
            if (colors.length() > 0) colors.append(":");
            colors.append(hex);
        }
        
        String mmString = String.format("<gradient:%s>%s</gradient>", colors.toString(), text);
        return miniMessage.deserialize(mmString);
    }

    /**
     * Désérialise un message MiniMessage classique.
     * Utile pour supporter les hex codes simples comme <#FF0000>Texte.
     *
     * @param text Le texte au format MiniMessage.
     * @return Le Component désérialisé.
     */
    public static Component colorize(String text) {
        return miniMessage.deserialize(text);
    }

    public static String format(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
