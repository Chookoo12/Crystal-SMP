package me.Chookoo.testPlugin.utils;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return false;

        Long time = playerCooldowns.get(ability);
        if (time == null) return false;

        return System.currentTimeMillis() < time;
    }

    public void setCooldown(UUID player, String ability, int ticks) {
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player, k -> new HashMap<>());
        long millis = System.currentTimeMillis() + ticks * 50L; // 1 tick = 50ms
        playerCooldowns.put(ability, millis);
    }

    public long getCooldownEnd(UUID player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return 0L;

        return playerCooldowns.getOrDefault(ability, 0L);
    }


    public int getPlayerCooldown(UUID player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return 0;

        Long time = playerCooldowns.get(ability);
        if (time == null) return 0;

        long remaining = time - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000);
    }

    public Map<String, Long> getCooldowns(UUID player) {
        Map<String, Long> map = cooldowns.get(player);
        if (map == null) return Collections.emptyMap();
        return new HashMap<>(map); // prevent modification
    }


    public boolean tryUseAbility(Player player, int xpCost, int minCooldown, int maxCooldown, String abilityName) {
        UUID uuid = player.getUniqueId();

        // Check cooldown first
        if (isOnCooldown(uuid, abilityName)) {
            int remaining = getPlayerCooldown(uuid, abilityName);
            player.sendMessage("⏳ Ability on cooldown: " + remaining + "s");
            return false;
        }

        // Then check XP
        if (player.getLevel() < xpCost) {
            player.sendMessage("❌ You need " + xpCost + " XP to use this ability!");
            return false;
        }

        // Deduct XP and set cooldown
        int cooldownSeconds = minCooldown + (int)(Math.random() * (maxCooldown - minCooldown + 1));
        setCooldown(uuid, abilityName, cooldownSeconds * 20);
        player.setLevel(player.getLevel() - xpCost);

        return true;

    }
    public boolean tryUseAbilityCooldownOnly(Player player, int minCooldown, int maxCooldown, String abilityName) {
        UUID uuid = player.getUniqueId();

        if (isOnCooldown(uuid, abilityName)) {
            int remaining = getPlayerCooldown(uuid, abilityName);
            player.sendMessage("⏳ Ability on cooldown: " + remaining + "s");
            return false;
        }

        int cooldownSeconds = minCooldown + (int)(Math.random() * (maxCooldown - minCooldown + 1));
        setCooldown(uuid, abilityName, cooldownSeconds * 20);
        return true;
    }


    public void clearCooldown(Player player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) playerCooldowns.remove(ability);


    }
}