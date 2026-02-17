package me.Chookoo.testPlugin.utils.HUD;

import me.Chookoo.testPlugin.Main;
import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.roll.OreType;
import me.Chookoo.testPlugin.utils.roll.PlayerClassManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ClassHUD {

    private final JavaPlugin plugin;
    private final CooldownManager cooldowns;

    public ClassHUD(JavaPlugin plugin, AbilityProvider abilityProvider) {
        this.plugin = plugin;
        this.cooldowns = ((Main) plugin).getCooldownManager();
        start();
    }

    private void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    render(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void render(Player player) {

        OreType type = PlayerClassManager.getClass(player.getUniqueId());

        if (type == null) {
            player.sendActionBar(Component.empty());
            return;
        }

        String left = "";
        String right = "";
        String center = getCenterIcon(type);

        UUID id = player.getUniqueId();

        switch (type) {

            case AMETHYST -> {
                left += formatLeftAbility('\uE008', cooldowns.getCooldownEnd(id, "amethyst_primary"));
            }

            case EMERALD -> {
                left += formatLeftAbility('\uE005', cooldowns.getCooldownEnd(id, "emerald_primary"));
            }

            case COPPER -> {
                left += formatLeftAbility('\uE001', cooldowns.getCooldownEnd(id, "copper_primary"));
            }

            case IRON -> {
                left += formatLeftAbility('\uE003', cooldowns.getCooldownEnd(id, "iron_primary"));
            }

            case GOLD -> {
                left += formatLeftAbility('\uE00B', cooldowns.getCooldownEnd(id, "gold_primary"));
            }

            case REDSTONE -> {
                left += formatLeftAbility('\uE010', cooldowns.getCooldownEnd(id, "redstone_primary"));
            }

            case LAPIS -> {
                left += formatLeftAbility('\uE009', cooldowns.getCooldownEnd(id, "lapis_primary"));
            }
        }

        String finalHUD = align(left, center, right);
        player.sendActionBar(Component.text(finalHUD));
    }

    private String formatLeftAbility(char icon, long endTime) {

        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        String cd = remaining <= 0 ? "§aReady!" : "§e" + remaining + "s";

        return "§r" + icon + " " + cd + "  ";
    }

    private String formatRightAbility(char icon, long endTime) {

        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        String cd = remaining <= 0 ? "§aReady!" : "§e" + remaining + "s";

        return "  " + cd + " §r" + icon;
    }

    private String getCenterIcon(OreType type) {
        return switch (type) {
            case COPPER -> "§r\uE001";
            case IRON -> "§r\uE003";
            case GOLD -> "§r\uE007";
            case EMERALD -> "§r\uE005";
            case REDSTONE -> "§r\uE010";
            case LAPIS -> "§r\uE009";
            case AMETHYST -> "§r\uE008";
        };
    }

    private String align(String left, String center, String right) {

        int leftZone = 35;   // space reserved left side
        int rightZone = 35;  // space reserved right side

        String paddedLeft = padEnd(left, leftZone);
        String paddedRight = padStart(right, rightZone);

        return paddedLeft + center + paddedRight;
    }

    private String padEnd(String text, int size) {
        if (text.length() >= size) return text;
        return text + " ".repeat(size - text.length());
    }

    private String padStart(String text, int size) {
        if (text.length() >= size) return text;
        return " ".repeat(size - text.length()) + text;
    }
}
