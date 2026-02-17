package me.Chookoo.testPlugin.utils.HUD;

public class AbilityDisplay {

    private final char icon;
    private final int seconds;
    private final boolean rightSide;

    public AbilityDisplay(char icon, int seconds, boolean rightSide) {
        this.icon = icon;
        this.seconds = seconds;
        this.rightSide = rightSide;
    }

    public char getIcon() {
        return icon;
    }

    public int getSeconds() {
        return seconds;
    }

    public boolean isRightSide() {
        return rightSide;
    }

    public boolean isReady() {
        return seconds <= 0;
    }
}
