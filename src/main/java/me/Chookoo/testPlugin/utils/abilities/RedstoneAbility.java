package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownTime, cooldownTime, "redstone"))
            return true;

        int remaining = cooldownManager.getPlayerCooldown(player.getUniqueId(), "redstone");
        startCooldownTimer(player, remaining);

        player.sendMessage(Component.text("🔴 Charging Redstone Overload...", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);

        // charge slowness
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                40, // 2 seconds (matches charge)
                1,  // amplifier don't do an Alfi
                false,
                false
        ));

        if (Bukkit.getPluginManager().isPluginEnabled("PlayerParticles")) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "ppo " + player.getName() +
                            " add dust_color_transition spiral red red"
            );
        }





        // charging
        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 40;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();

                    // Remove slowness
                    player.removePotionEffect(PotionEffectType.SLOWNESS);

                    if (Bukkit.getPluginManager().isPluginEnabled("PlayerParticles")) {
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "ppo " + player.getName() + " reset"
                        );
                    }

                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // shockwave
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
                            startRedstoneTickDamage(player, maxRadius, 3);
                            spawnGroundParticles(player.getLocation(), maxRadius, 3);
                            return;
                        }

                        for (Entity entity : player.getNearbyEntities(maxRadius, maxRadius, maxRadius)) {
                            if (!(entity instanceof LivingEntity target)) continue;
                            if (target.equals(player)) continue;
                            if (hitEntities.contains(target)) continue;

                            double distance = entity.getLocation().distance(player.getLocation());
                            if (distance <= radius && distance > radius - step) {
                                hitEntities.add(target);

                                target.setNoDamageTicks(0);
                                target.damage(0.1);
                                target.setHealth(Math.max(0, target.getHealth() - 5));

                                double kb = Math.max(1.3, 2.0 * (1 - distance / maxRadius));
                                Vector push = target.getLocation().toVector()
                                        .subtract(player.getLocation().toVector())
                                        .normalize()
                                        .multiply(kb);
                                push.setY(kb * 1);
                                target.setVelocity(push);
                            }
                        }

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

    // cd
    private void startCooldownTimer(Player player, int cooldownSeconds) {
        new BukkitRunnable() {
            int ticks = 0;
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (ticks >= cooldownSeconds * 20) {
                    player.sendActionBar(Component.text("✨ Redstone Ability ready!", NamedTextColor.GREEN));
                    cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    player.sendActionBar(Component.text(
                            "⏱ Redstone Cooldown: " + remaining + "s",
                            NamedTextColor.AQUA
                    ));
                    remaining--;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // tick damage
    private void startRedstoneTickDamage(Player caster, double radius, int durationSeconds) {
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

                    target.setNoDamageTicks(0);
                    target.damage(0.1);
                    target.setHealth(Math.max(0, target.getHealth() - 2));

                    Vector push = target.getLocation().toVector()
                            .subtract(caster.getLocation().toVector())
                            .normalize()
                            .multiply(0.3);
                    push.setY(0.1);
                    target.setVelocity(target.getVelocity().add(push));
                }

                seconds++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnGroundParticles(Location center, double radius, int durationSeconds) {
        new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (seconds >= durationSeconds) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 100; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;

                    center.getWorld().spawnParticle(
                            Particle.DUST_COLOR_TRANSITION,
                            center.clone().add(x, 0.25, z),
                            700,
                            0, 0, 0,
                            0,
                            new Particle.DustOptions(Color.RED, 1.2f)
                    );
                }

                seconds++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
