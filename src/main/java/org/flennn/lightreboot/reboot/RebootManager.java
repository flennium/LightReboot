package org.flennn.lightreboot.reboot;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.flennn.lightreboot.LightReboot;
import org.flennn.lightreboot.api.LightRebootPreShutdownEvent;
import org.flennn.lightreboot.config.RebootConfig;
import org.flennn.lightreboot.util.Console;
import org.flennn.lightreboot.util.Text;
import org.flennn.lightreboot.util.TimeParser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public final class RebootManager {
    private final LightReboot plugin;
    private final RebootConfig config;
    private BukkitRunnable countdownTask;
    private BukkitRunnable scheduleTask;
    private BossBar bossBar;
    private int secondsLeft;
    private int totalSeconds;
    private String lastScheduleKey = "";

    public RebootManager(LightReboot plugin, RebootConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean start(CommandSender sender, int delaySeconds, String reason) {
        if (isRunning()) {
            sender.sendMessage(config.message("already-running", "{time}", TimeParser.format(secondsLeft)));
            return false;
        }
        if (!config.isValidDelay(delaySeconds)) {
            sender.sendMessage(config.message(
                    "delay-out-of-range",
                    "{min}", String.valueOf(config.getMinDelay()),
                    "{max}", String.valueOf(config.getMaxDelay())
            ));
            return false;
        }

        totalSeconds = delaySeconds;
        secondsLeft = delaySeconds;
        createBossBar();

        Bukkit.broadcastMessage(config.message(
                "reboot-started",
                "{time}", TimeParser.format(delaySeconds),
                "{reason}", reason
        ));
        Console.info("Reboot started by " + sender.getName() + " for " + TimeParser.format(delaySeconds));

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
        return true;
    }

    public boolean cancel(CommandSender sender) {
        if (!isRunning()) {
            sender.sendMessage(config.message("not-running"));
            return false;
        }

        stopCountdown();
        Bukkit.broadcastMessage(config.message("reboot-cancelled", "{player}", sender.getName()));
        Console.warn("Reboot cancelled by " + sender.getName());
        return true;
    }

    public void reload(CommandSender sender) {
        config.reload();
        restartScheduler();
        sender.sendMessage(config.message("reload-success"));
    }

    public void preview(CommandSender sender, int seconds) {
        String time = TimeParser.format(Math.max(1, seconds));
        sender.sendMessage(config.message("preview-header", "{time}", time));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            sendTitle(player, time);
            sendActionBar(player, time);
        }

        sender.sendMessage(format(config.getBroadcast().getMessage(), null, time));
    }

    public void status(CommandSender sender) {
        if (isRunning()) {
            sender.sendMessage(config.message("status-running", "{time}", TimeParser.format(secondsLeft)));
            return;
        }

        RebootConfig.ScheduleSettings schedule = config.getSchedule();
        if (!schedule.isEnabled() || schedule.getTimes().isEmpty()) {
            sender.sendMessage(config.message("status-idle"));
            return;
        }

        sender.sendMessage(config.message(
                "status-scheduled",
                "{count}", String.valueOf(schedule.getTimes().size()),
                "{timezone}", schedule.getZoneId().getId()
        ));
    }

    public boolean isRunning() {
        return countdownTask != null;
    }

    public void startScheduler() {
        stopScheduler();
        RebootConfig.ScheduleSettings schedule = config.getSchedule();
        if (!schedule.isEnabled()) {
            Console.info("Scheduled reboots are disabled");
            return;
        }
        if (schedule.getTimes().isEmpty()) {
            Console.warn("Scheduled reboots are enabled, but no valid times are configured");
            return;
        }

        scheduleTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkSchedule();
            }
        };
        scheduleTask.runTaskTimer(plugin, 20L, 20L);
        Console.success("Scheduled reboots loaded: " + schedule.getTimes().size() + " time(s)");
    }

    public void restartScheduler() {
        startScheduler();
    }

    public void shutdown() {
        stopCountdown();
        stopScheduler();
    }

    private void tick() {
        if (secondsLeft <= 0) {
            stopCountdown();
            beginShutdown();
            return;
        }

        updateBossBar();
        if (secondsLeft == totalSeconds || config.getWarningTimes().contains(secondsLeft)) {
            sendCountdown(secondsLeft);
        } else {
            sendPassiveChannels(secondsLeft);
        }
        secondsLeft--;
    }

    private void sendCountdown(int seconds) {
        String time = TimeParser.format(seconds);
        if (config.getBroadcast().isEnabled()) {
            Bukkit.broadcastMessage(format(config.getBroadcast().getMessage(), null, time));
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, time);
            sendActionBar(player, time);
        }
    }

    private void sendPassiveChannels(int seconds) {
        String time = TimeParser.format(seconds);
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, time);
        }
    }

    private void sendTitle(Player player, String time) {
        if (!config.getTitle().isEnabled()) {
            return;
        }
        player.sendTitle(
                format(config.getTitle().getTitle(), player, time),
                format(config.getTitle().getSubtitle(), player, time),
                config.getTitle().getFadeIn(),
                config.getTitle().getStay(),
                config.getTitle().getFadeOut()
        );
    }

    private void sendActionBar(Player player, String time) {
        if (!config.getActionBar().isEnabled()) {
            return;
        }
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(format(config.getActionBar().getMessage(), player, time))
        );
    }

    private void createBossBar() {
        if (!config.getBossBar().isEnabled()) {
            return;
        }
        bossBar = Bukkit.createBossBar(
                format(config.getBossBar().getMessage(), null, TimeParser.format(totalSeconds)),
                parseBarColor(config.getBossBar().getColor()),
                parseBarStyle(config.getBossBar().getStyle())
        );
        bossBar.setVisible(true);
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(format(config.getBossBar().getMessage(), null, TimeParser.format(secondsLeft)));
        bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, secondsLeft / (double) totalSeconds)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void beginShutdown() {
        Console.warn("Reboot countdown finished. Starting shutdown flow.");
        Bukkit.broadcastMessage(config.message("shutdown-started"));
        Bukkit.getPluginManager().callEvent(new LightRebootPreShutdownEvent());

        if (config.shouldSavePlayers()) {
            savePlayers();
        }
        if (config.shouldSaveWorlds()) {
            saveWorlds();
        }
        if (config.shouldDisablePlugins()) {
            disableConfiguredPlugins();
        }
        if (config.shouldKickPlayers()) {
            kickPlayers();
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Console.warn("Calling Bukkit.shutdown()");
            Bukkit.shutdown();
        }, config.getFinalShutdownDelay() * 20L);
    }

    private void savePlayers() {
        try {
            Bukkit.savePlayers();
            Console.success("Saved player data");
        } catch (Exception e) {
            Console.error("Failed to save player data: " + e.getMessage());
        }
    }

    private void saveWorlds() {
        for (World world : Bukkit.getWorlds()) {
            try {
                world.save();
                Console.success("Saved world: " + world.getName());
            } catch (Exception e) {
                Console.error("Failed to save world " + world.getName() + ": " + e.getMessage());
            }
        }
    }

    private void disableConfiguredPlugins() {
        for (String pluginName : config.getPluginsToDisable()) {
            Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
            if (target == null || !target.isEnabled() || target == plugin) {
                continue;
            }
            try {
                Bukkit.getPluginManager().disablePlugin(target);
                Console.success("Disabled plugin: " + target.getName());
            } catch (Exception e) {
                Console.error("Failed to disable plugin " + pluginName + ": " + e.getMessage());
            }
        }
    }

    private void kickPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(config.plainMessage("kick-message", "{player}", player.getName()));
        }
        Console.info("Kicked online players");
    }

    private void checkSchedule() {
        if (isRunning()) {
            return;
        }

        RebootConfig.ScheduleSettings schedule = config.getSchedule();
        ZonedDateTime now = ZonedDateTime.now(schedule.getZoneId());
        if (!schedule.getDays().contains(now.getDayOfWeek())) {
            return;
        }

        LocalTime currentMinute = now.toLocalTime().withSecond(0).withNano(0);
        LocalDate currentDate = now.toLocalDate();
        for (LocalTime time : schedule.getTimes()) {
            if (time.equals(currentMinute)) {
                String key = currentDate + " " + time;
                if (!key.equals(lastScheduleKey)) {
                    lastScheduleKey = key;
                    start(Bukkit.getConsoleSender(), config.getDefaultDelay(), "scheduled");
                }
                return;
            }
        }
    }

    private void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        removeBossBar();
        secondsLeft = 0;
        totalSeconds = 0;
    }

    private void stopScheduler() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
    }

    private String format(String message, Player player, String time) {
        String withPlaceholders = Text.applyPlaceholders(
                message,
                "{time}", time,
                "{seconds}", String.valueOf(secondsLeft),
                "{online}", String.valueOf(Bukkit.getOnlinePlayers().size())
        );
        withPlaceholders = Text.parsePlaceholderApi(withPlaceholders, player);
        return config.color(withPlaceholders);
    }

    private BarColor parseBarColor(String value) {
        try {
            return BarColor.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            Console.warn("Invalid bossbar color '" + value + "', using YELLOW");
            return BarColor.YELLOW;
        }
    }

    private BarStyle parseBarStyle(String value) {
        try {
            return BarStyle.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            Console.warn("Invalid bossbar style '" + value + "', using SOLID");
            return BarStyle.SOLID;
        }
    }
}
