package org.flennn.lightreboot.util;

import org.bukkit.Bukkit;

public final class Text {
    private Text() {
    }

    public static String color(String message) {
        return Console.color(message);
    }

    public static String applyPlaceholders(String message, String... replacements) {
        String result = message == null ? "" : message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    public static String parsePlaceholderApi(String message, org.bukkit.entity.Player player) {
        if (message == null || player == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return message;
        }

        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object parsed = placeholderApi
                    .getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class)
                    .invoke(null, player, message);
            return parsed instanceof String ? (String) parsed : message;
        } catch (ReflectiveOperationException e) {
            Console.warn("PlaceholderAPI hook failed: " + e.getMessage());
            return message;
        }
    }
}
