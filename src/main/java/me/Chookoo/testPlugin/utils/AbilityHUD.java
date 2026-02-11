package me.Chookoo.testPlugin.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityHUD {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    private static final String COPPER_READY = "\uE001";
    private static final String COPPER_CD = "\uE002";

    private static final String IRON_READY = "\uE003";
    private static final String IRON_CD = "\uE004";

    private static final String GAP = "\u2007";

    // Tracks which ability is currently being displayed per player
    private final Map<UUID, String> activeAbility = new HashMap<>();

    // Prevents jitter
    private final Map<UUID, String> lastMessage = new HashMap<>();

    public AbilityHUD(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        start();
    }

    // Called by abilities when used
    public void trackAbility(Player player, String abilityKey) {
        activeAbility.put(player.getUniqueId(), abilityKey);
    }

    private void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    render(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // once per second (stable)
    }

    private void render(Player player) {

        UUID id = player.getUniqueId();
        String abilityKey = activeAbility.get(id);

        if (abilityKey == null) {
            clear(player);
            return;
        }

        int remaining = cooldownManager.getPlayerCooldown(id, abilityKey);

        String message;

        if (abilityKey.equals("copper")) {
            message = buildMessage(remaining, COPPER_READY, COPPER_CD);
        } else if (abilityKey.equals("iron")) {
            message = buildMessage(remaining, IRON_READY, IRON_CD);
        } else {
            return;
        }

        send(player, message);

        // Remove once ready message has shown
        if (remaining <= 0) {
            activeAbility.remove(id);
        }
    }

    private String buildMessage(int remaining, String readyIcon, String cdIcon) {

        if (remaining > 0) {
            return cdIcon + GAP +
                    (remaining < 10 ? " " : "") +
                    remaining + "s";
        }

        return readyIcon + GAP + "ready!";
    }

    private void send(Player player, String message) {

        UUID id = player.getUniqueId();

        if (message.equals(lastMessage.get(id))) return;

        lastMessage.put(id, message);
        player.sendActionBar(Component.text(message));
    }

    private void clear(Player player) {
        UUID id = player.getUniqueId();
        lastMessage.remove(id);
        player.sendActionBar(Component.text(""));
    }
}
