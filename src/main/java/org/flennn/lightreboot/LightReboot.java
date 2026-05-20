package org.flennn.lightreboot;

import org.bukkit.plugin.java.JavaPlugin;
import org.flennn.lightreboot.command.LightRebootCommand;
import org.flennn.lightreboot.config.RebootConfig;
import org.flennn.lightreboot.reboot.RebootManager;
import org.flennn.lightreboot.util.Console;

public final class LightReboot extends JavaPlugin {
    private RebootConfig rebootConfig;
    private RebootManager rebootManager;

    @Override
    public void onEnable() {
        Console.info("Starting LightReboot v" + getDescription().getVersion() + "...");

        saveDefaultConfig();
        rebootConfig = new RebootConfig(this);
        rebootConfig.reload();

        rebootManager = new RebootManager(this, rebootConfig);
        rebootManager.startScheduler();

        LightRebootCommand command = new LightRebootCommand(rebootManager, rebootConfig);
        if (getCommand("lightreboot") != null) {
            getCommand("lightreboot").setExecutor(command);
            getCommand("lightreboot").setTabCompleter(command);
            Console.success("Command registered");
        } 
        
        Console.success("LightReboot enabled");
    }

    @Override
    public void onDisable() {
        if (rebootManager != null) {
            rebootManager.shutdown();
        }
        Console.success("LightReboot disabled");
    }
}
