package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class IronAbility implements Listener {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    private final Map<UUID, Integer> hitsLeft = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> shields = new HashMap<>();
    private final Map<UUID, BukkitRunnable> shieldGlows = new HashMap<>();
    private final Map<UUID, PotionEffect> activeSlowness = new HashMap<>();

    private final int cooldownTime = 30; // seconds

    // Orbit settings
    private final double orbitRadius = 1.5;
    private final double[] heightOffsets = new double[]{0.5, 0.7, 0.9};
    private final double bobAmplitude = 0.2;
    private final double bobSpeed = 0.015;

    public IronAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    public void activate(Player player) {
        // Use cooldown-only method, no XP cost
        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownTime, cooldownTime, "iron")) {
            player.sendMessage(Component.text("Ability is on cooldown!", NamedTextColor.RED));
            return;
        }

        if (hitsLeft.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Iron Shield already active!", NamedTextColor.RED));
            return;
        }

        hitsLeft.put(player.getUniqueId(), 3);

        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 35, 1, false, false, true);
        player.addPotionEffect(slowness);
        activeSlowness.put(player.getUniqueId(), slowness);

        spawnShields(player);

        // Use action-bar cooldown instead of XP bar
        int remainingCooldown = cooldownManager.getPlayerCooldown(player.getUniqueId(), "iron");
        startCooldownIndicator(player, remainingCooldown, "Iron Ability");

        startShieldGlow(player);

        player.sendMessage(Component.text("🛡 Iron Shield activated!", NamedTextColor.GRAY));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        if (!hitsLeft.containsKey(id)) return;

        event.setCancelled(true);

        int remaining = hitsLeft.get(id) - 1;

        if (remaining > 0) {
            hitsLeft.put(id, remaining);
            removeOneShield(player);
            spawnCrackEffect(player);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1.2f);
            player.sendMessage(Component.text("🛡 Shield absorbed hit! (" + remaining + " left)", NamedTextColor.GRAY));
        } else {
            hitsLeft.remove(id);
            removeAllShields(player);
            stopShieldGlow(player);

            if (activeSlowness.containsKey(id)) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                activeSlowness.remove(id);
            }

            explode(player);
        }
    }

    private void spawnShields(Player player) {
        List<ArmorStand> list = new ArrayList<>();
        World world = player.getWorld();

        for (int i = 0; i < 3; i++) {
            ArmorStand stand = (ArmorStand) world.spawnEntity(
                    player.getLocation().clone().add(0, heightOffsets[i], 0),
                    EntityType.ARMOR_STAND
            );
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.getEquipment().setHelmet(new ItemStack(Material.IRON_BLOCK));
            list.add(stand);
        }

        shields.put(player.getUniqueId(), list);

        new BukkitRunnable() {
            double angle = 0;
            double bob = 0;
            boolean up = true;

            @Override
            public void run() {
                if (!hitsLeft.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                angle += Math.PI / 60;
                bob += up ? bobSpeed : -bobSpeed;
                if (bob > bobAmplitude) up = false;
                if (bob < -bobAmplitude) up = true;

                List<ArmorStand> stands = shields.get(player.getUniqueId());
                if (stands == null) return;

                for (int i = 0; i < stands.size(); i++) {
                    double a = angle + (2 * Math.PI / stands.size()) * i;
                    double x = Math.cos(a) * orbitRadius;
                    double z = Math.sin(a) * orbitRadius;

                    Location target = player.getLocation().clone().add(0, heightOffsets[i] + bob, 0).add(x, 0, z);
                    ArmorStand stand = stands.get(i);

                    Location current = stand.getLocation();
                    double dx = (target.getX() - current.getX()) * 0.3;
                    double dy = (target.getY() - current.getY()) * 0.3;
                    double dz = (target.getZ() - current.getZ()) * 0.3;

                    current.add(dx, dy, dz);
                    stand.teleport(current);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void removeOneShield(Player player) {
        List<ArmorStand> list = shields.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return;
        ArmorStand stand = list.remove(0);
        stand.remove();
    }

    private void removeAllShields(Player player) {
        List<ArmorStand> list = shields.remove(player.getUniqueId());
        if (list != null) list.forEach(Entity::remove);
    }

    private void startShieldGlow(Player player) {
        BukkitRunnable glowTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hitsLeft.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().clone().add(0, 1.5, 0);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 5, 0.5, 0.5, 0.5, 0.05);
            }
        };
        glowTask.runTaskTimer(plugin, 0L, 2L);
        shieldGlows.put(player.getUniqueId(), glowTask);
    }

    private void stopShieldGlow(Player player) {
        BukkitRunnable glowTask = shieldGlows.remove(player.getUniqueId());
        if (glowTask != null) glowTask.cancel();
    }

    private void spawnCrackEffect(Player player) {
        Location loc = player.getLocation().clone().add(0, 1.5, 0);
        player.getWorld().spawnParticle(
                Particle.BLOCK_CRUMBLE,
                loc,
                50,
                0.5, 0.5, 0.5,
                0.1,
                Material.IRON_BLOCK.createBlockData()
        );
    }

    private void explode(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        world.spawnParticle(Particle.EXPLOSION, loc, 2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2);
        world.spawnParticle(Particle.CRIT, loc, 50, 1.5, 1.0, 1.5, 0.1);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);
        world.playSound(loc, Sound.BLOCK_ANVIL_BREAK, 1f, 0.8f);
        world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 1f, 1.2f);

        double knockbackRadius = 3.5;

        for (Entity e : world.getNearbyEntities(loc, knockbackRadius, knockbackRadius, knockbackRadius)) {
            if (e instanceof Damageable d && !e.equals(player)) {
                d.damage(4.0);
                Vector direction = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.multiply(1.2);
                e.setVelocity(direction.setY(0.5));
            }
        }

        player.sendMessage(Component.text("💥 Iron Shield shattered!", NamedTextColor.RED));
    }

    private void startCooldownIndicator(Player player, int cooldownSeconds, String abilityName) {
        new BukkitRunnable() {
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    player.sendActionBar(Component.text("✨ " + abilityName + " ready!", NamedTextColor.GRAY));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text("⏳ " + abilityName + " cooldown: " + remaining + "s", NamedTextColor.RED));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
