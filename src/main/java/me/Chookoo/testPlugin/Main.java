package me.Chookoo.testPlugin;

import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.HUD.AbilityProvider;
import me.Chookoo.testPlugin.utils.abilities.AbilityCommandExecutor;
import me.Chookoo.testPlugin.utils.roll.PlayerClassManager;
import me.Chookoo.testPlugin.utils.roll.RollCommand;
import me.Chookoo.testPlugin.utils.items.RerollListener;
import me.Chookoo.testPlugin.utils.items.RerollToken;
import me.Chookoo.testPlugin.utils.HUD.ClassHUD;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin {

    private CooldownManager cooldownManager;
    private AbilityProvider abilityProvider;
    public static NamespacedKey keyRerollToken;

    @Override
    public void onEnable() {



        cooldownManager = new CooldownManager();

        saveDefaultConfig();

        PlayerClassManager.init(this);

        // Commands
        if (getCommand("ability") != null) {
            getCommand("ability").setExecutor(
                    new AbilityCommandExecutor(this, cooldownManager)
            );
        }

        if (getCommand("roll") != null) {
            getCommand("roll").setExecutor(new RollCommand(this));
        }

        keyRerollToken = new NamespacedKey(this, "reroll_token");


        // Register Reroll Listener
        getServer().getPluginManager().registerEvents(new RerollListener(this), this);

        // Register crafting recipe
        registerRecipes();

        abilityProvider = new AbilityProvider(cooldownManager);
        new ClassHUD(this, abilityProvider);


        getLogger().info("CrystalSMP Plugin Enabled!");
    }

    private void registerRecipes() {

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(this, "reroll_token"),
                RerollToken.create()
        );

        recipe.shape("ETE", "ADA", "ETE");

        recipe.setIngredient('A', Material.NETHER_STAR);
        recipe.setIngredient('E', Material.DIAMOND_BLOCK);
        recipe.setIngredient('D', Material.NETHERITE_BLOCK);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);

        Bukkit.addRecipe(recipe);
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
