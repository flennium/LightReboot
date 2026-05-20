package org.flennn.lightreboot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.flennn.lightreboot.config.RebootConfig;
import org.flennn.lightreboot.reboot.RebootManager;
import org.flennn.lightreboot.util.TimeParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LightRebootCommand implements CommandExecutor, TabCompleter {
    private final RebootManager rebootManager;
    private final RebootConfig config;

    public LightRebootCommand(RebootManager rebootManager, RebootConfig config) {
        this.rebootManager = rebootManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (TimeParser.parseSeconds(subCommand) >= 0) {
            String[] startArgs = new String[args.length + 1];
            startArgs[0] = "start";
            System.arraycopy(args, 0, startArgs, 1, args.length);
            return start(sender, startArgs);
        }

        switch (subCommand) {
            case "start":
                return start(sender, args);
            case "cancel":
                return cancel(sender);
            case "reload":
                return reload(sender);
            case "status":
                return status(sender);
            case "preview":
                return preview(sender, args);
            default:
                sender.sendMessage(config.message("unknown-command"));
                sendHelp(sender);
                return true;
        }
    }

    private boolean start(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lightreboot.start")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        int delay = args.length >= 2 ? TimeParser.parseSeconds(args[1]) : config.getDefaultDelay();
        if (delay < 0) {
            sender.sendMessage(config.message("delay-invalid"));
            return true;
        }

        String reason = args.length >= 3 ? join(args, 2) : "manual";
        rebootManager.start(sender, delay, reason);
        return true;
    }

    private boolean cancel(CommandSender sender) {
        if (!sender.hasPermission("lightreboot.cancel")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        rebootManager.cancel(sender);
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("lightreboot.reload")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        rebootManager.reload(sender);
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!sender.hasPermission("lightreboot.status")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        rebootManager.status(sender);
        return true;
    }

    private boolean preview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lightreboot.preview")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        int delay = args.length >= 2 ? TimeParser.parseSeconds(args[1]) : config.getDefaultDelay();
        if (delay < 0) {
            sender.sendMessage(config.message("delay-invalid"));
            return true;
        }

        rebootManager.preview(sender, delay);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(config.plainMessage("help-header"));
        sender.sendMessage(config.color("&e/lightreboot start [time] [reason] &7- Start a reboot countdown"));
        sender.sendMessage(config.color("&e/lightreboot cancel &7- Cancel the active countdown"));
        sender.sendMessage(config.color("&e/lightreboot status &7- Show reboot status"));
        sender.sendMessage(config.color("&e/lightreboot preview [time] &7- Preview countdown messages"));
        sender.sendMessage(config.color("&e/lightreboot reload &7- Reload config.yml"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            addIfAllowed(sender, options, "start", "lightreboot.start");
            addIfAllowed(sender, options, "cancel", "lightreboot.cancel");
            addIfAllowed(sender, options, "status", "lightreboot.status");
            addIfAllowed(sender, options, "preview", "lightreboot.preview");
            addIfAllowed(sender, options, "reload", "lightreboot.reload");
            options.add("help");
            return filter(options, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("preview"))) {
            return filter(Arrays.asList("30s", "1m", "5m", "10m"), args[1]);
        }

        return Collections.emptyList();
    }

    private void addIfAllowed(CommandSender sender, List<String> options, String value, String permission) {
        if (sender.hasPermission(permission)) {
            options.add(value);
        }
    }

    private List<String> filter(List<String> options, String input) {
        List<String> results = new ArrayList<>();
        String lower = input.toLowerCase();
        for (String option : options) {
            if (option.startsWith(lower)) {
                results.add(option);
            }
        }
        return results;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
