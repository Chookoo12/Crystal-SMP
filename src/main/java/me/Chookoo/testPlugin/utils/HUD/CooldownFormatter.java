package me.Chookoo.testPlugin.utils.HUD;

public class CooldownFormatter {

    public static String format(long endTime) {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;

        if (remaining <= 0) return "§aReady";
        return "§e" + remaining + "s";
    }
}
