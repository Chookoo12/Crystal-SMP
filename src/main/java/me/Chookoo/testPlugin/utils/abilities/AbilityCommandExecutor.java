package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.AbilityHUD;
import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AbilityCommandExecutor implements CommandExecutor {

    private final CopperAbility copperAbility;
    private final IronAbility ironAbility;
    private final AbilityHUD abilityHUD;
    private final CooldownManager cooldownManager;

    public AbilityCommandExecutor(JavaPlugin plugin, CooldownManager cooldownManager, AbilityHUD abilityHUD) {
        this.copperAbility = new CopperAbility(plugin, cooldownManager);
        this.ironAbility = new IronAbility(plugin, cooldownManager);
        this.cooldownManager = cooldownManager;
        this.abilityHUD = abilityHUD;

        // Register Iron listener
        plugin.getServer().getPluginManager().registerEvents(ironAbility, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /ability <copper|iron> <level>", NamedTextColor.YELLOW));
            return true;
        }

        String abilityName = args[0].toLowerCase();

        switch (abilityName) {
            case "copper" -> {
                copperAbility.onCommand(sender, command, label, args);

                // Only mark for HUD if it's on cooldown
                if (cooldownManager.getPlayerCooldown(player.getUniqueId(), "copper") > 0) {
                    abilityHUD.markAbilityUsed(player, "copper");
                }
            }
            case "iron" -> {
                ironAbility.activate(player);

                if (cooldownManager.getPlayerCooldown(player.getUniqueId(), "iron") > 0) {
                    abilityHUD.markAbilityUsed(player, "iron");
                }
            }
            default -> player.sendMessage(
                    Component.text("Unknown ability. Use: copper, iron", NamedTextColor.RED)
            );
        }

        return true;
    }
}
