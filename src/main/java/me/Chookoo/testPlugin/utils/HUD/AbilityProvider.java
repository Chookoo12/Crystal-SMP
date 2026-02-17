package me.Chookoo.testPlugin.utils.HUD;

import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.roll.OreType;
import me.Chookoo.testPlugin.utils.roll.PlayerClassManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AbilityProvider {

    private final CooldownManager cooldowns;

    public AbilityProvider(CooldownManager cooldowns) {
        this.cooldowns = cooldowns;
    }

    public List<AbilityDisplay> getAbilities(Player player) {

        List<AbilityDisplay> abilities = new ArrayList<>();

        UUID uuid = player.getUniqueId();
        OreType type = PlayerClassManager.getClass(uuid);

        if (type == null) return abilities;

        switch (type) {

            case AMETHYST ->
                    abilities.add(new AbilityDisplay(
                            '\uE021',
                            cooldowns.getPlayerCooldown(uuid, "amethyst_primary"),
                            false
                    ));

            case EMERALD ->
                    abilities.add(new AbilityDisplay(
                            '\uE022',
                            cooldowns.getPlayerCooldown(uuid, "emerald_primary"),
                            false
                    ));

            case COPPER ->
                    abilities.add(new AbilityDisplay(
                            '\uE023',
                            cooldowns.getPlayerCooldown(uuid, "copper_primary"),
                            false
                    ));

            case IRON ->
                    abilities.add(new AbilityDisplay(
                            '\uE024',
                            cooldowns.getPlayerCooldown(uuid, "iron_primary"),
                            false
                    ));

            case GOLD ->
                    abilities.add(new AbilityDisplay(
                            '\uE025',
                            cooldowns.getPlayerCooldown(uuid, "gold_primary"),
                            false
                    ));

            case REDSTONE ->
                    abilities.add(new AbilityDisplay(
                            '\uE026',
                            cooldowns.getPlayerCooldown(uuid, "redstone_primary"),
                            false
                    ));

            case LAPIS ->
                    abilities.add(new AbilityDisplay(
                            '\uE027',
                            cooldowns.getPlayerCooldown(uuid, "lapis_primary"),
                            false
                    ));
        }

        return abilities;
    }
}
