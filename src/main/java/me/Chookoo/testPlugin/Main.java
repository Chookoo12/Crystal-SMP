package me.Chookoo.testPlugin;

import me.Chookoo.testPlugin.utils.AbilityHUD;
import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.abilities.AbilityCommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private CooldownManager cooldownManager;
    private AbilityHUD abilityHUD;

    @Override
    public void onEnable() {

        cooldownManager = new CooldownManager();
        abilityHUD = new AbilityHUD(this, cooldownManager);

        if (getCommand("ability") != null) {
            getCommand("ability").setExecutor(
                    new AbilityCommandExecutor(this, cooldownManager, abilityHUD)
            );
        }

        getCommand("ability").setExecutor(
                new AbilityCommandExecutor(this, cooldownManager, abilityHUD)
        );


        getLogger().info("Plugin enabled successfully!");
    }

    public AbilityHUD getAbilityHUD() {
        return abilityHUD;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
