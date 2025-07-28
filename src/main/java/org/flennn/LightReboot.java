package org.flennn;

import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class LightReboot extends JavaPlugin implements org.bukkit.command.CommandExecutor {

    private List<String> pluginsToDisable;
    private FileConfiguration config;
    private BukkitRunnable rebootTask = null;
    private int rebootDelay = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLightRebootConfig();
        this.getCommand("reboot").setExecutor(this);
        this.getCommand("lightrebootreload").setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (rebootTask != null) {
            rebootTask.cancel();
            rebootTask = null;
        }
        Bukkit.getScheduler().cancelTasks(this);
    }

    private void reloadLightRebootConfig() {
        reloadConfig();
        config = getConfig();
        pluginsToDisable = config.getStringList("plugins-to-disable");
    }

    private String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String getMessage(String key) {
        return colorize(config.getString("messages." + key, ""));
    }

    private String getMessage(String key, String placeholder, String value) {
        return colorize(config.getString("messages." + key, "").replace(placeholder, value));
    }

    public static class LightRebootPreShutdownEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        @Override
        public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lightrebootreload")) {
            if (!sender.hasPermission("lightreboot.reload")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
            reloadLightRebootConfig();
            sender.sendMessage(getMessage("reload-success"));
            return true;
        }
        if (!command.getName().equalsIgnoreCase("reboot")) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("lightreboot.cancel")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
            if (rebootTask != null) {
                rebootTask.cancel();
                rebootTask = null;
                Bukkit.broadcastMessage(getMessage("cancel-broadcast"));
            } else {
                sender.sendMessage(getMessage("usage"));
            }
            return true;
        }
        if (!sender.hasPermission("lightreboot.reboot")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(getMessage("usage"));
            return true;
        }
        int delay;
        try {
            delay = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("delay-invalid"));
            return true;
        }
        if (delay < 1 || delay > 30) {
            sender.sendMessage(getMessage("delay-out-of-range"));
            return true;
        }
        if (rebootTask != null) {
            sender.sendMessage(colorize("&cA reboot is already pending. Use /reboot cancel to abort."));
            return true;
        }
        rebootDelay = delay;
        Bukkit.broadcastMessage(getMessage("reboot-broadcast", "{seconds}", String.valueOf(delay)));
        rebootTask = new BukkitRunnable() {
            int secondsLeft = delay;
            @Override
            public void run() {
                if (secondsLeft == 0) {
                    Bukkit.getPluginManager().callEvent(new LightRebootPreShutdownEvent());
                    for (String pluginName : pluginsToDisable) {
                        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
                        if (plugin != null && plugin.isEnabled()) {
                            try {
                                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                                Bukkit.broadcastMessage(getMessage("plugin-disabled", "{plugin}", pluginName));
                                getLogger().info("Disabled plugin: " + pluginName);
                            } catch (Exception ex) {
                                getLogger().severe("Failed to disable plugin: " + pluginName + " - " + ex.getMessage());
                            }
                        }
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try {
                                    player.kickPlayer(getMessage("kick-message"));
                                    getLogger().info("Kicked player: " + player.getName());
                                } catch (Exception ex) {
                                    getLogger().severe("Failed to kick player: " + player.getName() + " - " + ex.getMessage());
                                }
                            }
                            try {
                                Bukkit.getServer().savePlayers();
                                getLogger().info("Player data saved.");
                            } catch (Exception ex) {
                                getLogger().severe("Failed to save player data: " + ex.getMessage());
                            }
                            for (World world : Bukkit.getServer().getWorlds()) {
                                try {
                                    world.setAutoSave(true);
                                    world.save();
                                    getLogger().info("World saved: " + world.getName());
                                } catch (Exception ex) {
                                    getLogger().severe("Failed to save world: " + world.getName() + " - " + ex.getMessage());
                                }
                            }
                            try {
                                Bukkit.getServer().savePlayers();
                                getLogger().info("Final player data save before shutdown.");
                            } catch (Exception ex) {
                                getLogger().severe("Final player data save failed: " + ex.getMessage());
                            }
                            for (World world : Bukkit.getServer().getWorlds()) {
                                try {
                                    world.save();
                                    getLogger().info("Final world save before shutdown: " + world.getName());
                                } catch (Exception ex) {
                                    getLogger().severe("Final world save failed: " + world.getName() + " - " + ex.getMessage());
                                }
                            }
                            Bukkit.shutdown();
                            rebootTask = null;
                        }
                    }.runTaskLater(LightReboot.this, 20L); // 1 second delay
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("", getMessage("notify-warning", "{seconds}", String.valueOf(secondsLeft)), 0, 25, 5);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(getMessage("notify-warning", "{seconds}", String.valueOf(secondsLeft))));
                }
                if (secondsLeft <= 5 || secondsLeft % 5 == 0) {
                    Bukkit.broadcastMessage(getMessage("reboot-warning", "{seconds}", String.valueOf(secondsLeft)));
                }
                secondsLeft--;
            }
        };
        rebootTask.runTaskTimer(this, 20L, 20L);
        return true;
    }
}
