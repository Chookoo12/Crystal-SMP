package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CopperAbility implements CommandExecutor {

    // Copper HUD icons (resource pack)
    private static final String COPPER_READY_ICON = "\uE001";
    private static final String COPPER_CD_ICON = "\uE002";

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;
    private static final int COOLDOWN_TIME = 10;

    public CopperAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("copper") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability copper 1", NamedTextColor.YELLOW));
            return true;
        }

        if (!cooldownManager.tryUseAbility(player, 0, COOLDOWN_TIME, COOLDOWN_TIME, "copper")) return true;

        startCooldownIndicator(
                player,
                cooldownManager.getPlayerCooldown(player.getUniqueId(), "copper")
        );

        PlayerInventory inv = player.getInventory();
        boolean copperHelmet =
                inv.getHelmet() != null &&
                        inv.getHelmet().getType().toString().contains("COPPER");

        boolean inWater = player.getLocation().getBlock().isLiquid();

        // buff strike
        if (player.isSneaking() && player.getEyeLocation().getDirection().getY() < -0.5) {

            // WATER MODE
            if (inWater) {
                player.getWorld().strikeLightningEffect(player.getLocation());
                electrifyWater(player, player.getLocation(), 5, copperHelmet);
                player.sendMessage(Component.text("🌊 The water becomes electrified!", NamedTextColor.AQUA));
                return true;
            }

            // LAND BUFF
            if (copperHelmet) {
                player.getWorld().strikeLightning(player.getLocation());
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 6, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 6, 1));
                player.sendMessage(Component.text("⚡ Copper power surges through you!", NamedTextColor.GOLD));
                return true;
            }
        }

        // main attack
        LivingEntity target = findTarget(player, 15);

        if (target == null) {
            player.sendMessage(Component.text("Missed.", NamedTextColor.YELLOW));
            return true;
        }

        player.sendMessage(Component.text("⚡ Copper ability activated!", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            int strikes = 0;

            @Override
            public void run() {
                if (strikes >= 2) {
                    cancel();
                    return;
                }
                playLightningStrike(target);
                strikes++;
            }
        }.runTaskTimer(plugin, 0L, 40L);

        return true;
    }

    // lightning attack
    private void playLightningStrike(LivingEntity target) {

        target.getWorld().playSound(
                target.getLocation(),
                Sound.ITEM_TRIDENT_THUNDER,
                2f,
                1.5f
        );


        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick < 20) {
                    int particles = 30 + tick;
                    double radius = 1 + tick * 0.03;

                    for (int i = 0; i < particles; i++) {
                        double angle = 2 * Math.PI * i / particles;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        target.getWorld().spawnParticle(
                                Particle.ELECTRIC_SPARK,
                                target.getLocation().add(x, 0.15, z),
                                1, 0, 0, 0, 0.05
                        );
                    }
                    tick++;
                } else {
                    target.getWorld().strikeLightning(target.getLocation());

// Force damage animation + red flash
                    target.setNoDamageTicks(0);
                    target.damage(0.1);

// True damage (ignores armor)
                    double trueDamage = 3.5;
                    target.setHealth(Math.max(0, target.getHealth() - trueDamage));


                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ELECTRIFIED WATER
    private void electrifyWater(Player caster, Location center, int durationSeconds, boolean amplified) {

        // ===== CUSTOMIZE HERE =====
        int radius = 20;
        int particleCountPerSecond = 1000;
        double particleHeight = 2;
        int glowingDuration = 20 * 7;
        int slownessDuration = 20 * 7;
        int weaknessDuration = 20 * 7;

        double trueDamagePerSecond = 2;

        World world = center.getWorld();

        new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (seconds >= durationSeconds) {
                    cancel();
                    return;
                }

                // particles
                for (int i = 0; i < particleCountPerSecond; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * radius * 2;
                    double y = center.getY() + Math.random() * particleHeight;
                    double z = center.getZ() + (Math.random() - 0.5) * radius * 2;

                    world.spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
                    world.spawnParticle(Particle.BUBBLE_POP, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
                }

                for (Entity e : world.getNearbyEntities(center, radius, 3, radius)) {
                    if (!(e instanceof LivingEntity living)) continue;

                    if (living.getUniqueId().equals(caster.getUniqueId())) continue;

                    Location loc = living.getLocation();

                    if (loc.getBlock().getType() == Material.WATER ||
                            loc.clone().subtract(0, 1, 0).getBlock().getType() == Material.WATER) {

                        // TRUE DAMAGE
                        living.setNoDamageTicks(0);
                        living.setHealth(Math.max(0, living.getHealth() - trueDamagePerSecond));
                        living.setNoDamageTicks(0);
                        living.damage(0.1);

                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.GLOWING, glowingDuration, 0, false, false, true));

                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, slownessDuration, 1, false, false, true));

                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.WEAKNESS, weaknessDuration, 1, false, false, true));
                    }
                }

                seconds++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // targeting
    private LivingEntity findTarget(Player player, double maxDistance) {
        for (Entity e : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == player) continue;

            Vector toTarget = living.getLocation().toVector()
                    .subtract(player.getEyeLocation().toVector());

            if (toTarget.length() > maxDistance) continue;

            double angle = toTarget.normalize()
                    .angle(player.getEyeLocation().getDirection());

            if (angle < Math.toRadians(30)) {
                return living;
            }
        }
        return null;
    }

    // Keep track of the active cooldown task to prevent multiple overlapping tasks
    private BukkitRunnable cooldownTask;

    private void startCooldownIndicator(Player player, int cooldownSeconds) {
        // Show ready icon first
        player.sendActionBar(Component.text(COPPER_READY_ICON + "\u2007ready!"));

        // Cancel previous task if any
        if (cooldownTask != null) cooldownTask.cancel();

        cooldownTask = new BukkitRunnable() {
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (remaining > 0) {
                    // Pad single-digit numbers with a space for alignment
                    String secondsText = (remaining < 10 ? " " : "") + remaining;
                    // Figure space (\u2007) keeps text aligned vertically with icon
                    player.sendActionBar(Component.text(COPPER_CD_ICON + "\u2007" + secondsText + "s"));
                    remaining--;
                } else {
                    // Cooldown finished: show ready icon + text
                    player.sendActionBar(Component.text(COPPER_READY_ICON + "\u2007ready!"));
                    cancel();
                }
            }
        };

        cooldownTask.runTaskTimer(plugin, 0L, 20L);
    }
}