package me.Chookoo.testPlugin.utils.roll;

import net.kyori.adventure.text.format.NamedTextColor;

public enum OreType {

    EMERALD("Emerald", NamedTextColor.GREEN),
    GOLD("Gold", NamedTextColor.GOLD),
    IRON("Iron", NamedTextColor.WHITE),
    LAPIS("Lapis", NamedTextColor.BLUE),
    COPPER("Copper", NamedTextColor.GOLD),
    REDSTONE("Redstone", NamedTextColor.RED),
    AMETHYST("Amethyst", NamedTextColor.LIGHT_PURPLE);

    private final String display;
    private final NamedTextColor color;

    OreType(String display, NamedTextColor color) {
        this.display = display;
        this.color = color;
    }

    public String getDisplay() { return display; }
    public NamedTextColor getColor() { return color; }

    public static OreType random() {
        OreType[] values = values();
        return values[(int) (Math.random() * values.length)];
    }
}
