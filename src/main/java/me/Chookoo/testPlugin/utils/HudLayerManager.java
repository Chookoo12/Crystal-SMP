package me.Chookoo.testPlugin.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple HUD manager to store temporary action bar layers per player.
 */
public class HudLayerManager {

    private final JavaPlugin plugin;
    private final Map<Player, Map<String, String>> layers = new HashMap<>();

    public HudLayerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void set(Player player, String key, String text) {
        layers.computeIfAbsent(player, k -> new HashMap<>()).put(key, text);
    }

    public void remove(Player player, String key) {
        Map<String, String> map = layers.get(player);
        if (map != null) map.remove(key);
    }

    public String get(Player player, String key) {
        Map<String, String> map = layers.get(player);
        if (map != null) return map.get(key);
        return null;
    }
}
