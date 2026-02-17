package me.Chookoo.testPlugin.utils.roll;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class RollCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    // Map each OreType to a command that should run after rolling
    private final Map<OreType, String> commandsOnRoll = new HashMap<>();

    public RollCommand(JavaPlugin plugin) {
        this.plugin = plugin;

        // Add your commands here!
        commandsOnRoll.put(OreType.COPPER, "ppo %player% add wax_on hurt");
        commandsOnRoll.put(OreType.IRON, "ppo %player% add block hurt iron_block iron_block");
        commandsOnRoll.put(OreType.GOLD, "ppo %player% add block hurt gold_block gold_block");
        commandsOnRoll.put(OreType.LAPIS, "ppo %player% add block hurt lapis_block lapis_block");
        commandsOnRoll.put(OreType.EMERALD, "ppo %player% add happy_villager hurt");
        commandsOnRoll.put(OreType.REDSTONE, "ppo %player% add dust_color_transition hurt red red");
        commandsOnRoll.put(OreType.AMETHYST, "ppo %player% add block hurt amethyst_block amethyst_block");
    }

    public static void rollPlayer(JavaPlugin plugin, Player player) {
        new RollCommand(plugin).startRoll(player);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage("You must be OP to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("/roll <player|@e|@s>");
            return true;
        }

        Collection<Player> targets = getTargets(sender, args[0]);

        if (targets.isEmpty()) {
            sender.sendMessage("No valid players found.");
            return true;
        }

        for (Player player : targets) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ppo " + player.getName() + " reset");

            startRoll(player);
        }

        return true;
    }

    private Collection<Player> getTargets(CommandSender sender, String arg) {

        if (arg.equalsIgnoreCase("@e"))
            return new ArrayList<>(Bukkit.getOnlinePlayers());

        if (arg.equalsIgnoreCase("@s") && sender instanceof Player)
            return Collections.singleton((Player) sender);

        Player target = Bukkit.getPlayer(arg);
        return target != null ? Collections.singleton(target) : Collections.emptyList();
    }

    public void startRoll(Player player) {

        int cycles = plugin.getConfig().getInt("roll.cycles", 40);
        int speed = plugin.getConfig().getInt("roll.speed", 2);

        Random random = new Random();

        // Get player's current class (if any)
        OreType previousClass = PlayerClassManager.getClass(player.getUniqueId());

        // Build list of possible rolls
        List<OreType> possibleRolls = new ArrayList<>(Arrays.asList(OreType.values()));

        // If player already has a class, remove it from possibilities
        if (previousClass != null) {
            possibleRolls.remove(previousClass);
        }

        // Pick final roll from filtered list
        final OreType finalRoll = possibleRolls.get(random.nextInt(possibleRolls.size()));

        new BukkitRunnable() {

            int current = 0;

            @Override
            public void run() {

                if (current < cycles) {

                    // Show random ore from possible list (so it doesn't show old class)
                    OreType showing = possibleRolls.get(random.nextInt(possibleRolls.size()));

                    player.showTitle(
                            Title.title(
                                    Component.text("Rolling... ", NamedTextColor.GRAY)
                                            .append(Component.text(showing.getDisplay(), showing.getColor())),
                                    Component.empty(),
                                    Title.Times.times(
                                            Duration.ofMillis(100),
                                            Duration.ofMillis(speed * 50),
                                            Duration.ofMillis(50)
                                    )
                            )
                    );

                    current++;
                    return;
                }

                // Save new class
                PlayerClassManager.setClass(player.getUniqueId(), finalRoll);

                player.showTitle(
                        Title.title(
                                Component.text(finalRoll.getDisplay(), finalRoll.getColor()),
                                Component.text("You rolled a NEW ability!", NamedTextColor.GRAY),
                                Title.Times.times(
                                        Duration.ofMillis(300),
                                        Duration.ofMillis(2000),
                                        Duration.ofMillis(800)
                                )
                        )
                );

                if (possibleRolls.isEmpty()) {
                    player.sendMessage("No other classes available to reroll into.");
                    return;
                }

                player.sendMessage(
                        Component.text("You are now a ")
                                .append(Component.text(finalRoll.getDisplay(), finalRoll.getColor()))
                );

                cancel();
            }

        }.runTaskTimer(plugin, 0L, speed);
    }
}