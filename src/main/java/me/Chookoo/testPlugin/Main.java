package me.Chookoo.testPlugin;

import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.abilities.AbilityCommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {

        // Managers
        cooldownManager = new CooldownManager();

        // Register commands
        if (getCommand("ability") != null) {
            getCommand("ability").setExecutor(
                    new AbilityCommandExecutor(this, cooldownManager)
            );
        }

        getLogger().info("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
