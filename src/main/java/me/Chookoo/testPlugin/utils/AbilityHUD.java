package me.Chookoo.testPlugin.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AbilityHUD {

    private final JavaPlugin plugin;
    private final CooldownManager cooldowns;

    private final Map<UUID, Map<String, Integer>> fadeMap = new HashMap<>();
    private final Map<UUID, String> lastBar = new HashMap<>();

    private static final int FADE_TIME = 40;
    private static final String PX = "\uE101";

    public AbilityHUD(JavaPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        start();
    }

    private void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    render(p);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void markAbilityUsed(Player player, String ability) {
        UUID id = player.getUniqueId();
        fadeMap.computeIfAbsent(id, k -> new HashMap<>()).put(ability, 0);
    }

    private void render(Player player) {
        UUID id = player.getUniqueId();
        Map<String, Integer> playerFades = fadeMap.get(id);

        if (playerFades == null || playerFades.isEmpty()) {
            clear(player);
            return;
        }

        StringBuilder leftBar = new StringBuilder();
        StringBuilder centerBar = new StringBuilder();

        List<String> toRemove = new ArrayList<>();

        // Stable order (iron first)
        List<String> abilities = new ArrayList<>(playerFades.keySet());
        abilities.sort((a, b) -> a.equals("iron") ? -1 : b.equals("iron") ? 1 : a.compareTo(b));

        // Detect if multiple abilities are fading
        boolean multiple = abilities.size() > 1;

        for (String ability : abilities) {

            int fade = playerFades.getOrDefault(ability, 0);
            int seconds = cooldowns.getPlayerCooldown(id, ability);
            boolean ready = seconds <= 0;

            // ===== GROUP FADE LOGIC =====
            if (ready) {
                fade++;

                // If multiple abilities exist, clamp removal until ALL finished
                if (!multiple && fade > FADE_TIME) {
                    toRemove.add(ability);
                    continue;
                }

                playerFades.put(ability, fade);
            }

            String icon = icon(ability, ready);
            String text = ready ? "ready " : seconds + "s ";

            String part;

            // LEFT SIDE: text before icon
            if (ability.equals("iron")) {
                part = text + PX + icon + PX + PX;
                leftBar.append(part);
            }
            // CENTER: icon before text
            else {
                part = icon + PX + text + PX + PX;
                centerBar.append(part);
            }
        }

        // After rendering — synchronized removal
        if (multiple) {
            boolean allFinished = true;
            for (int f : playerFades.values())
                if (f <= FADE_TIME) allFinished = false;

            if (allFinished) {
                playerFades.clear();
            }
        } else {
            for (String remove : toRemove)
                playerFades.remove(remove);
        }

        String finalBar = leftBar.toString() + centerBar.toString();

        if (!finalBar.equals(lastBar.get(id))) {
            lastBar.put(id, finalBar);
            player.sendActionBar(Component.text(finalBar));
        }

        if (playerFades.isEmpty()) {
            fadeMap.remove(id);
        }
    }

    private String icon(String ability, boolean ready) {
        return switch (ability) {
            case "copper" -> ready ? "\uE001" : "\uE002";
            case "iron" -> ready ? "\uE003" : "\uE004";
            default -> "?";
        };
    }

    private void clear(Player player) {
        lastBar.remove(player.getUniqueId());
        player.sendActionBar(Component.text(""));
    }
}
