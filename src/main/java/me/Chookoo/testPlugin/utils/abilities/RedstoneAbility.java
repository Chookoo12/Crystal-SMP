package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        // --- NEW cooldown system ---
        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownTime, cooldownTime, "redstone")) return true;

        // Show action-bar countdown for cooldown
        int remaining = cooldownManager.getPlayerCooldown(player.getUniqueId(), "redstone");
        startCooldownTimer(player, remaining);

        // --- ORIGINAL ABILITY LOGIC START ---
        player.sendMessage(Component.text("🔴 Charging Redstone Overload...", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.8f, 1f);

        BukkitRunnable chargingParticles = new BukkitRunnable() {
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
                            1,
                            new Particle.DustOptions(org.bukkit.Color.RED, 1)
                    );
                }
                ticks++;
            }
        };
        chargingParticles.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                new BukkitRunnable() {
                    double radius = 0.1;
                    final double maxRadius = 7;
                    final double step = 0.5;
                    Set<LivingEntity> hitEntities = new HashSet<>();

                    @Override
                    public void run() {
                        if (radius > maxRadius) {
                            cancel();
                            return;
                        }
                        for (Entity entity : player.getNearbyEntities(maxRadius, maxRadius, maxRadius)) {
                            if (entity instanceof LivingEntity target &&
                                    entity != player &&
                                    !hitEntities.contains(target)) {

                                double distance = entity.getLocation().distance(player.getLocation());
                                if (distance <= radius && distance > radius - step) {
                                    hitEntities.add(target);

                                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 5, 4));
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 7, 2));

                                    if (target instanceof Player) {
                                        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 5, 1));
                                    }

                                    double knockbackMultiplier = 2.0 * (1 - distance / maxRadius);
                                    if (knockbackMultiplier < 0.3) knockbackMultiplier = 0.3;

                                    Vector push = target.getLocation().toVector()
                                            .subtract(player.getLocation().toVector())
                                            .normalize()
                                            .multiply(knockbackMultiplier);
                                    push.setY(knockbackMultiplier * 0.5);
                                    target.setVelocity(push);
                                }
                            }
                        }

                        for (int i = 0; i < 50; i++) {
                            double angle = 2 * Math.PI * i / 50;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    player.getLocation().add(x, 1, z),
                                    1,
                                    new Particle.DustOptions(org.bukkit.Color.RED, 1)
                            );
                        }

                        radius += step;
                    }
                }.runTaskTimer(plugin, 0L, 2L);

            }
        }.runTaskLater(plugin, 40L);
        // --- ORIGINAL ABILITY LOGIC END ---

        return true;
    }

    // --- NEW cooldown timer function ---
    private void startCooldownTimer(Player player, int cooldownSeconds) {
        new BukkitRunnable() {
            int ticksPassed = 0;
            int totalTicks = cooldownSeconds * 20;
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
}
