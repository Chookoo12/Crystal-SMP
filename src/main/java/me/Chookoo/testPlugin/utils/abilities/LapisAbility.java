package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class LapisAbility implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    private static final int COOLDOWN_TIME = 10;

    public LapisAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("lapis") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability lapis 1", NamedTextColor.YELLOW));
            return true;
        }

        if (!cooldownManager.tryUseAbility(player, 0, COOLDOWN_TIME, COOLDOWN_TIME, "lapis"))
            return true;

        int remainingCooldown = cooldownManager.getPlayerCooldown(player.getUniqueId(), "lapis");
        startCooldownIndicator(player, remainingCooldown, "Lapis Ability");

        player.sendMessage(Component.text("🔮 Charging Arcane Blast...", NamedTextColor.BLUE));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 3f, 1f);

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS,
                60,
                10,
                false,
                false,
                false
        ));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    return;
                }

                double radius = 1.0 + ticks * 0.05;

                for (int i = 0; i < 50; i++) {
                    double angle = 2 * Math.PI * i / 50;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    player.getWorld().spawnParticle(
                            Particle.DUST,
                            player.getLocation().add(x, 0.05, z),
                            1,
                            new Particle.DustOptions(Color.BLUE, 1f)
                    );
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (int i = 0; i < 3; i++) {
            int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    fireRaycastProjectile(player, index);
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1f);
                }
            }.runTaskLater(plugin, i * 20L);
        }

        return true;
    }

    private void fireRaycastProjectile(Player shooter, int index) {
        Location start = shooter.getEyeLocation().clone();
        Vector direction = rotateVector(start.getDirection().normalize(), (index - 1) * 5);

        Set<LivingEntity> hitEntities = new HashSet<>();
        double step = 0.5;
        double maxDistance = 30;

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                if (traveled >= maxDistance) {
                    cancel();
                    return;
                }

                start.add(direction.clone().multiply(step));
                traveled += step;

                shooter.getWorld().spawnParticle(
                        Particle.DUST,
                        start,
                        2,
                        0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(80, 120, 255), 1.2f)
                );

                double spiralRadius = 0.2;
                double heightAmplitude = 0.2;
                int strands = 4;

                for (int i = 0; i < strands; i++) {
                    double angle = (traveled * 30 + i * (360.0 / strands)) * Math.PI / 180;
                    double x = Math.cos(angle) * spiralRadius;
                    double y = Math.sin(angle) * heightAmplitude;
                    double z = Math.sin(angle) * spiralRadius;

                    shooter.getWorld().spawnParticle(
                            Particle.END_ROD,
                            start.clone().add(x, y, z),
                            1,
                            0, 0, 0, 0
                    );
                }

                for (LivingEntity target : start.getWorld().getNearbyEntities(start, 1.8, 1.8, 1.8).stream()
                        .filter(e -> e instanceof LivingEntity)
                        .map(e -> (LivingEntity) e)
                        .toList()) {

                    if (target == shooter || hitEntities.contains(target)) continue;

                    if (target instanceof Damageable damageable) {

                        // Tiny real damage for red flash + hurt animation
                        damageable.damage(0.1, shooter);

                        // Custom true damage (ignores armor)
                        double trueDamage = 2.5;
                        damageable.setHealth(Math.max(0, damageable.getHealth() - trueDamage));

                        hitEntities.add(target);


                        // ===== KNOCKBACK (NEW) =====
                        Vector knockback = target.getLocation().toVector()
                                .subtract(shooter.getLocation().toVector())
                                .normalize()
                                .multiply(0.7);   // horizontal strength (Punch I ~0.7, Punch II ~1.1)

                        knockback.setY(0.35); // vertical lift
                        target.setVelocity(knockback);
                        // ===========================

                        Location hitLoc = target.getLocation().add(0, 1, 0);

                        shooter.getWorld().spawnParticle(
                                Particle.DUST,
                                hitLoc,
                                30,
                                0.15, 0.15, 0.15,
                                0,
                                new Particle.DustOptions(Color.fromRGB(80, 120, 255), 2.2f)
                        );

                        shooter.getWorld().spawnParticle(
                                Particle.END_ROD,
                                hitLoc,
                                8,
                                0.3, 0.3, 0.3,
                                0.05
                        );

                        shooter.getWorld().playSound(
                                hitLoc,
                                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                                3f,
                                1.6f
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector rotateVector(Vector vector, double yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;

        return new Vector(x, vector.getY(), z).normalize();
    }

    private void startCooldownIndicator(Player player, int cooldownSeconds, String abilityName) {
        new BukkitRunnable() {
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    player.sendActionBar(Component.text("✨ " + abilityName + " ready!", NamedTextColor.BLUE));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text("⏳ " + abilityName + " cooldown: " + remaining + "s", NamedTextColor.RED));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
