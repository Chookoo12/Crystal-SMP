package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class RedstoneAbility implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;
    private final int cooldownTime = 20; // seconds

    public RedstoneAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("redstone") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability redstone 1", NamedTextColor.YELLOW));
            return true;
        }

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownTime, cooldownTime, "redstone")) return true;

        int remaining = cooldownManager.getPlayerCooldown(player.getUniqueId(), "redstone");
        startCooldownTimer(player, remaining);

        player.sendMessage(Component.text("🔴 Charging Redstone Overload...", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);

        // --- Charging Particles ---
        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 40;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 50; i++) {
                    double angle = 2 * Math.PI * i / 50;
                    double x = Math.cos(angle) * 1.2;
                    double z = Math.sin(angle) * 1.2;
                    player.getWorld().spawnParticle(
                            Particle.DUST,
                            player.getLocation().add(x, 1, z),
                            2,
                            new Particle.DustOptions(Color.RED, 1)
                    );
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // --- Shockwave Explosion ---
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                new BukkitRunnable() {
                    double radius = 0.1;
                    final double maxRadius = 7;
                    final double step = 0.4;
                    final Set<LivingEntity> hitEntities = new HashSet<>();

                    @Override
                    public void run() {
                        if (radius > maxRadius) {
                            cancel();
                            startRedstoneTickDamage(player, maxRadius, 3); // seconds of Tick damage
                            return;
                        }

                        for (Entity entity : player.getNearbyEntities(maxRadius, maxRadius, maxRadius)) {
                            if (!(entity instanceof LivingEntity target)) continue;
                            if (target.equals(player)) continue;
                            if (hitEntities.contains(target)) continue;

                            double distance = entity.getLocation().distance(player.getLocation());
                            if (distance <= radius && distance > radius - step) {
                                hitEntities.add(target);

                                // Flash red briefly
                                target.setNoDamageTicks(0);
                                target.damage(0.1);

                                // True damage
                                double trueDamage = 2.5;
                                target.setHealth(Math.max(0, target.getHealth() - trueDamage));

                                // Initial knockback
                                double kbMultiplier = 2.0 * (1 - distance / maxRadius);
                                if (kbMultiplier < 0.3) kbMultiplier = 0.3;
                                Vector push = target.getLocation().toVector()
                                        .subtract(player.getLocation().toVector())
                                        .normalize()
                                        .multiply(kbMultiplier);
                                push.setY(kbMultiplier * 0.5);
                                target.setVelocity(push);
                            }
                        }

                        // Shockwave particles
                        for (int i = 0; i < 50; i++) {
                            double angle = 2 * Math.PI * i / 50;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    player.getLocation().add(x, 1, z),
                                    4,
                                    new Particle.DustOptions(Color.RED, 1)
                            );
                        }

                        radius += step;
                    }
                }.runTaskTimer(plugin, 0L, 2L);

            }
        }.runTaskLater(plugin, 40L);

        return true;
    }

    // --- Cooldown ActionBar ---
    private void startCooldownTimer(Player player, int cooldownSeconds) {
        new BukkitRunnable() {
            int ticksPassed = 0;
            final int totalTicks = cooldownSeconds * 20;
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (ticksPassed < totalTicks) {
                    if (ticksPassed % 20 == 0) {
                        player.sendActionBar(Component.text("⏱ Redstone Cooldown: " + remaining + "s", NamedTextColor.AQUA));
                        remaining--;
                    }
                    ticksPassed++;
                } else {
                    player.sendActionBar(Component.text("✨ Redstone Ability ready!", NamedTextColor.GREEN));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // tick damage
    private void startRedstoneTickDamage(Player caster, double radius, int durationSeconds) {
        final double trueDamagePerSecond = 1.5;

        new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (seconds >= durationSeconds) {
                    cancel();
                    return;
                }

                for (Entity e : caster.getWorld().getNearbyEntities(caster.getLocation(), radius, radius, radius)) {
                    if (!(e instanceof LivingEntity target)) continue;
                    if (target.equals(caster)) continue;

                    // damage
                    target.setNoDamageTicks(0);
                    target.damage(0.1);

                    target.setHealth(Math.max(0, target.getHealth() - trueDamagePerSecond));

                    // small kb
                    Vector push = target.getLocation().toVector()
                            .subtract(caster.getLocation().toVector())
                            .normalize()
                            .multiply(0.3); // adjustable small push
                    push.setY(0.1);
                    target.setVelocity(target.getVelocity().add(push));
                }

                seconds++;
            }
        }.runTaskTimer(plugin, 0L, 15L); // every second
    }
}
