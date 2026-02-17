package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.roll.OreType;
import me.Chookoo.testPlugin.utils.roll.PlayerClassManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class EmeraldAbility implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;
    private final int cooldownTime = 20;


    public EmeraldAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length != 2 || !args[0].equalsIgnoreCase("emerald_primary") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability emerald 1", NamedTextColor.YELLOW));
            return true;
        }

        if (PlayerClassManager.getClass(player.getUniqueId()) != OreType.EMERALD) {
            player.sendMessage(Component.text("You are not an Emerald user!", NamedTextColor.RED));
            return true;
        }

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownTime, cooldownTime, "emerald_primary"))
            return true;

        startCooldownTimer(player, cooldownTime);

        player.sendMessage(Component.text("💚 Unleashing Emerald Fang Wave...", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.6f, 1f);

        // ---- Adjustable Parameters ----
        double stepDistance = 1.5;
        double totalDistance = 5;
        double spreadWidth = 3;     // total horizontal width
        int fangsPerRow = 4;
        int totalRows = 4;            // horizontal rows
        double rowSpacing = 1.0;
        double trueDamage = 0.15;
        long fangLifetime = 25L;
        // --------------------------------

        launchFangWave(player, stepDistance, totalDistance, spreadWidth,
                fangsPerRow, totalRows, rowSpacing, trueDamage, fangLifetime);

        return true;
    }

    private void launchFangWave(Player player,
                                double stepDistance,
                                double totalDistance,
                                double spreadWidth,
                                int fangsPerRow,
                                int totalRows,
                                double rowSpacing,
                                double trueDamage,
                                long lifetime) {

        Vector forward = player.getLocation().getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location origin = player.getLocation();

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                if (traveled >= totalDistance) {
                    cancel();
                    return;
                }

                for (int row = 0; row < totalRows; row++) {
                    double rowOffset = (row - (totalRows - 1) / 2.0) * rowSpacing;

                    Location rowCenter = origin.clone()
                            .add(forward.clone().multiply(traveled))
                            .add(right.clone().multiply(rowOffset));

                    for (int i = 0; i < fangsPerRow; i++) {
                        double offset = spreadWidth * (i / (double)(fangsPerRow - 1) - 0.5);
                        Location fangLoc = rowCenter.clone().add(right.clone().multiply(offset));

                        Location grounded = findGround(fangLoc);
                        if (grounded != null) {
                            spawnFang(grounded, player, trueDamage, lifetime);
                        }
                    }

                    spawnGreenParticles(rowCenter, spreadWidth);
                }

                traveled += stepDistance;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private Location findGround(Location start) {
        Location loc = start.clone();
        for (int i = 0; i < 6; i++) {
            if (loc.getBlock().getType().isSolid()) {
                return loc.add(0, 1, 0);
            }
            loc.subtract(0, 1, 0);
        }
        return null;
    }

    private void spawnFang(Location loc, Player caster, double damage, long lifetime) {
        EvokerFangs fangs = loc.getWorld().spawn(loc, EvokerFangs.class);

        Set<LivingEntity> hit = new HashSet<>();
        loc.getWorld().getNearbyEntities(loc, 1.5, 1.2, 1.5).forEach(e -> {
            if (!(e instanceof LivingEntity target)) return;
            if (target.equals(caster)) return;
            if (!hit.add(target)) return;


            target.damage(0.1);
            target.setHealth(Math.max(0, target.getHealth() - damage));

            if (target instanceof Player hitPlayer) {
                float current = hitPlayer.getSaturation();
                hitPlayer.setSaturation(Math.max(0f, current - 0.35f));
            }

            Vector kb = target.getLocation().toVector()
                    .subtract(caster.getLocation().toVector())
                    .normalize()
                    .multiply(0.4);
            kb.setY(0.2);
            target.setVelocity(kb);
        });

        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                fangs.remove();
            }
        }.runTaskLater(plugin, lifetime);
    }

    private void spawnGreenParticles(Location center, double width) {
        center.getWorld().spawnParticle(
                Particle.DUST,
                center,
                12,
                width / 4,
                0.2,
                width / 4,
                new Particle.DustOptions(Color.fromRGB(50, 220, 100), 1.4f)
        );
    }

    private void startCooldownTimer(Player player, int seconds) {
        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                if (time <= 0) {
                    player.sendActionBar(Component.text("✨ Emerald Ability ready!", NamedTextColor.GREEN));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text("⏱ Emerald Cooldown: " + time + "s", NamedTextColor.AQUA));
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
