package com.chatillon7.tameablelimit;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TameablePlaceholders extends PlaceholderExpansion {
    private final TameableLimitPlugin plugin;

    public TameablePlaceholders(TameableLimitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "tameable";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        if (identifier.equals("tamed_count")) {
            int total = 0;
            for (String mobType : new String[]{"WOLF", "CAT", "HORSE", "DONKEY", "MULE", 
                                             "LLAMA", "PARROT", "TRADER_LLAMA", "CAMEL"}) {
                total += plugin.getTameableCount(player.getUniqueId(), mobType);
            }
            return String.valueOf(total);
        }

        if (identifier.endsWith("_count")) {
            String mobType = identifier.replace("_count", "").toUpperCase();
            return String.valueOf(plugin.getTameableCount(player.getUniqueId(), mobType));
        }

        return null;
    }
}
