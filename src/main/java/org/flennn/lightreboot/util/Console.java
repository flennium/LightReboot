package org.flennn.lightreboot.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class Console {
    private static final String PREFIX = "&8[&eLightReboot&8] ";

    private Console() {
    }

    public static void info(String message) {
        send("&bINFO", message);
    }

    public static void success(String message) {
        send("&aOK", message);
    }

    public static void warn(String message) {
        send("&eWARN", message);
    }

    public static void error(String message) {
        send("&cERROR", message);
    }

    private static void send(String level, String message) {
        Bukkit.getConsoleSender().sendMessage(color(PREFIX + level + " &7" + message));
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
