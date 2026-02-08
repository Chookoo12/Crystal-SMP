package me.Chookoo.testPlugin.utils.abilities;

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
    private final GoldAbility goldAbility;
    private final IronAbility ironAbility;
    private final RedstoneAbility redstoneAbility;
    private final LapisAbility lapisAbility;

    public AbilityCommandExecutor(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.copperAbility = new CopperAbility(plugin, cooldownManager);
        this.goldAbility = new GoldAbility(plugin, cooldownManager);
        this.ironAbility = new IronAbility(plugin, cooldownManager);
        this.redstoneAbility = new RedstoneAbility(plugin, cooldownManager);
        this.lapisAbility = new LapisAbility(plugin, cooldownManager);

        plugin.getServer().getPluginManager().registerEvents(ironAbility, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /ability <copper|gold|iron|redstone> <level>", NamedTextColor.YELLOW));
            return true;
        }

        String abilityName = args[0].toLowerCase();

        switch (abilityName) {
            case "copper" -> copperAbility.onCommand(sender, command, label, args);
            case "gold" -> goldAbility.onCommand(sender, command, label, args);
            case "iron" -> ironAbility.activate(player);
            case "redstone" -> redstoneAbility.onCommand(sender, command, label, args);
            case "lapis" -> lapisAbility.onCommand(sender,command, label, args);
            default -> player.sendMessage(
                    Component.text("Unknown ability. Use: copper, gold, iron, or redstone.", NamedTextColor.RED)
            );
        }

        return true;
    }
}
