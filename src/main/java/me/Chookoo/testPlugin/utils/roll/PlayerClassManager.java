package me.Chookoo.testPlugin.utils.roll;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerClassManager {

    private static final Map<UUID, OreType> classes = new HashMap<>();
    private static File file;
    private static FileConfiguration config;

    public static void init(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "classes.yml");

        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }

        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            OreType type = OreType.valueOf(config.getString(key));
            classes.put(uuid, type);
        }
    }

    public static void save() {
        classes.forEach((uuid, type) ->
                config.set(uuid.toString(), type.name()));

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public static void setClass(UUID uuid, OreType type) {
        classes.put(uuid, type);
        save();
    }

    public static OreType getClass(UUID uuid) {
        return classes.get(uuid);
    }

    public static boolean hasClass(UUID uuid, OreType type) {
        return classes.get(uuid) == type;
    }
}
