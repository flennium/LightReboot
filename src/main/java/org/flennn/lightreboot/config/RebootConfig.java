package org.flennn.lightreboot.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.flennn.lightreboot.LightReboot;
import org.flennn.lightreboot.util.Console;
import org.flennn.lightreboot.util.Text;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class RebootConfig {
    private final LightReboot plugin;
    private FileConfiguration config;

    private String prefix;
    private int defaultDelay;
    private int minDelay;
    private int maxDelay;
    private int finalShutdownDelay;
    private boolean kickPlayers;
    private boolean savePlayers;
    private boolean saveWorlds;
    private boolean disablePlugins;
    private List<String> pluginsToDisable;
    private Set<Integer> warningTimes;
    private ChannelSettings broadcast;
    private ChannelSettings title;
    private ChannelSettings actionBar;
    private BossBarSettings bossBar;
    private ScheduleSettings schedule;

    public RebootConfig(LightReboot plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();

        prefix = color(config.getString("messages.prefix", "&8[&eLightReboot&8]&r "));
        minDelay = clamp(config.getInt("reboot.min-delay-seconds", 5), 1, 86400);
        maxDelay = clamp(config.getInt("reboot.max-delay-seconds", 3600), minDelay, 86400);
        defaultDelay = clamp(config.getInt("reboot.default-delay-seconds", 60), minDelay, maxDelay);
        finalShutdownDelay = clamp(config.getInt("reboot.final-shutdown-delay-seconds", 3), 0, 60);
        kickPlayers = config.getBoolean("reboot.kick-players", true);
        savePlayers = config.getBoolean("reboot.save-players", true);
        saveWorlds = config.getBoolean("reboot.save-worlds", true);
        disablePlugins = config.getBoolean("reboot.disable-plugins.enabled", false);
        pluginsToDisable = cleanList(config.getStringList("reboot.disable-plugins.names"));
        warningTimes = parseWarningTimes(config.getIntegerList("countdown.warning-times"));

        broadcast = new ChannelSettings(
                config.getBoolean("countdown.broadcast.enabled", true),
                config.getString("countdown.broadcast.message", "&eServer restart in &c{time}&e.")
        );
        title = new ChannelSettings(
                config.getBoolean("countdown.title.enabled", true),
                config.getString("countdown.title.title", "&cRestarting Soon"),
                config.getString("countdown.title.subtitle", "&7Restart in &e{time}&7."),
                clamp(config.getInt("countdown.title.fade-in", 0), 0, 200),
                clamp(config.getInt("countdown.title.stay", 30), 1, 400),
                clamp(config.getInt("countdown.title.fade-out", 10), 0, 200)
        );
        actionBar = new ChannelSettings(
                config.getBoolean("countdown.actionbar.enabled", true),
                config.getString("countdown.actionbar.message", "&eRestart in &c{time}")
        );
        bossBar = new BossBarSettings(
                config.getBoolean("countdown.bossbar.enabled", true),
                config.getString("countdown.bossbar.message", "&eRestart in &c{time}"),
                config.getString("countdown.bossbar.color", "YELLOW"),
                config.getString("countdown.bossbar.style", "SOLID")
        );
        schedule = loadSchedule();

        validatePlugins();
        Console.success("Configuration loaded");
    }

    public String message(String key, String... replacements) {
        String raw = config.getString("messages." + key, "");
        return color(prefix + Text.applyPlaceholders(raw, replacements));
    }

    public String plainMessage(String key, String... replacements) {
        String raw = config.getString("messages." + key, "");
        return color(Text.applyPlaceholders(raw, replacements));
    }

    public String color(String message) {
        return Text.color(message);
    }

    public boolean isValidDelay(int seconds) {
        return seconds >= minDelay && seconds <= maxDelay;
    }

    public int getDefaultDelay() {
        return defaultDelay;
    }

    public int getMinDelay() {
        return minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public int getFinalShutdownDelay() {
        return finalShutdownDelay;
    }

    public boolean shouldKickPlayers() {
        return kickPlayers;
    }

    public boolean shouldSavePlayers() {
        return savePlayers;
    }

    public boolean shouldSaveWorlds() {
        return saveWorlds;
    }

    public boolean shouldDisablePlugins() {
        return disablePlugins;
    }

    public List<String> getPluginsToDisable() {
        return pluginsToDisable;
    }

    public Set<Integer> getWarningTimes() {
        return warningTimes;
    }

    public ChannelSettings getBroadcast() {
        return broadcast;
    }

    public ChannelSettings getTitle() {
        return title;
    }

    public ChannelSettings getActionBar() {
        return actionBar;
    }

    public BossBarSettings getBossBar() {
        return bossBar;
    }

    public ScheduleSettings getSchedule() {
        return schedule;
    }

    private ScheduleSettings loadSchedule() {
        boolean enabled = config.getBoolean("schedule.enabled", false);
        ZoneId zoneId = parseZone(config.getString("schedule.timezone", ZoneId.systemDefault().getId()));
        List<LocalTime> times = new ArrayList<>();
        for (String value : config.getStringList("schedule.times")) {
            try {
                times.add(LocalTime.parse(value));
            } catch (DateTimeException e) {
                Console.warn("Invalid schedule time ignored: " + value);
            }
        }

        Set<DayOfWeek> days = parseDays(config.getStringList("schedule.days"));
        return new ScheduleSettings(enabled, zoneId, times, days);
    }

    private ZoneId parseZone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException e) {
            Console.warn("Invalid timezone '" + value + "', using server default");
            return ZoneId.systemDefault();
        }
    }

    private Set<DayOfWeek> parseDays(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String value : values) {
            try {
                days.add(DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                Console.warn("Invalid schedule day ignored: " + value);
            }
        }
        return days.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : days;
    }

    private Set<Integer> parseWarningTimes(List<Integer> values) {
        Set<Integer> times = new TreeSet<>(Collections.reverseOrder());
        for (Integer value : values) {
            if (value != null && value > 0 && value <= maxDelay) {
                times.add(value);
            }
        }
        if (times.isEmpty()) {
            Collections.addAll(times, 300, 120, 60, 30, 10, 5, 4, 3, 2, 1);
            times.removeIf(value -> value > maxDelay);
        }
        return times;
    }

    private List<String> cleanList(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                cleaned.add(value.trim());
            }
        }
        return Collections.unmodifiableList(cleaned);
    }

    private void validatePlugins() {
        if (!disablePlugins) {
            return;
        }
        for (String pluginName : pluginsToDisable) {
            if (pluginName.equalsIgnoreCase(plugin.getName())) {
                Console.warn("Ignoring self-disable entry: " + pluginName);
            } else if (plugin.getServer().getPluginManager().getPlugin(pluginName) == null) {
                Console.warn("Plugin in disable list is not installed: " + pluginName);
            }
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static final class ChannelSettings {
        private final boolean enabled;
        private final String message;
        private final String title;
        private final String subtitle;
        private final int fadeIn;
        private final int stay;
        private final int fadeOut;

        public ChannelSettings(boolean enabled, String message) {
            this(enabled, message, message, 0, 30, 10);
        }

        public ChannelSettings(boolean enabled, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.enabled = enabled;
            this.message = subtitle;
            this.title = title;
            this.subtitle = subtitle;
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getMessage() {
            return message;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeOut() {
            return fadeOut;
        }
    }

    public static final class BossBarSettings {
        private final boolean enabled;
        private final String message;
        private final String color;
        private final String style;

        public BossBarSettings(boolean enabled, String message, String color, String style) {
            this.enabled = enabled;
            this.message = message;
            this.color = color;
            this.style = style;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getMessage() {
            return message;
        }

        public String getColor() {
            return color;
        }

        public String getStyle() {
            return style;
        }
    }

    public static final class ScheduleSettings {
        private final boolean enabled;
        private final ZoneId zoneId;
        private final List<LocalTime> times;
        private final Set<DayOfWeek> days;

        public ScheduleSettings(boolean enabled, ZoneId zoneId, List<LocalTime> times, Set<DayOfWeek> days) {
            this.enabled = enabled;
            this.zoneId = zoneId;
            this.times = Collections.unmodifiableList(new ArrayList<>(times));
            this.days = Collections.unmodifiableSet(days);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public List<LocalTime> getTimes() {
            return times;
        }

        public Set<DayOfWeek> getDays() {
            return days;
        }
    }
}
