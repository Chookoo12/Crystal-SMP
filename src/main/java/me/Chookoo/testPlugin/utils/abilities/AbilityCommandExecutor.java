package me.Chookoo.testPlugin.utils.abilities;

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
    private final GoldAbility goldAbility;
    private final RedstoneAbility redstoneAbility;
    private final LapisAbility lapisAbility;
    private final EmeraldAbility emeraldAbility;
    private final AmethystAbility amethystAbility;

    public AbilityCommandExecutor(JavaPlugin plugin, me.Chookoo.testPlugin.utils.CooldownManager cooldownManager) {

        // Create ability instances
        this.copperAbility = new CopperAbility(plugin, cooldownManager);
        this.ironAbility = new IronAbility(plugin, cooldownManager);
        this.goldAbility = new GoldAbility(plugin, cooldownManager);
        this.redstoneAbility = new RedstoneAbility(plugin, cooldownManager);
        this.lapisAbility = new LapisAbility(plugin, cooldownManager);
        this.emeraldAbility = new EmeraldAbility(plugin, cooldownManager);
        this.amethystAbility = new AmethystAbility(plugin, cooldownManager);

        // Register listeners (abilities that need events)
        plugin.getServer().getPluginManager().registerEvents(ironAbility, plugin);
        plugin.getServer().getPluginManager().registerEvents(amethystAbility, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /ability <copper|iron|gold|redstone|lapis|emerald|amethyst> <level>",
                    NamedTextColor.YELLOW
            ));
            return true;
        }

        String abilityName = args[0].toLowerCase();

        switch (abilityName) {
            case "copper" -> copperAbility.onCommand(sender, command, label, args);
            case "iron" -> ironAbility.activate(player);
            case "gold" -> goldAbility.onCommand(sender, command, label, args);
            case "redstone" -> redstoneAbility.onCommand(sender, command, label, args);
            case "lapis" -> lapisAbility.onCommand(sender, command, label, args);
            case "emerald" -> emeraldAbility.onCommand(sender, command, label, args);
            case "amethyst" -> amethystAbility.onCommand(sender, command, label, args);
            default -> player.sendMessage(Component.text(
                    "Unknown ability. Use: copper, iron, gold, redstone, lapis, emerald, or amethyst.",
                    NamedTextColor.RED
            ));
        }

        return true;
    }
}
