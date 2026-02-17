package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import me.Chookoo.testPlugin.utils.roll.OreType;
import me.Chookoo.testPlugin.utils.roll.PlayerClassManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AmethystAbility implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    // Adjustable parameters
    private final int cooldownSeconds = 20;
    private final int chargeSeconds = 4;
    private final double storedDamagePercent = 1;
    private final double damageMultiplier = 1.4;
    private final double blastRadius = 4;
    private final double maxBlastDamage = 15;
    private final double knockbackReduction = 0.35;

    // Bubble config
    private static final float BUBBLE_SIZE = 1.75f;
    private static final float BUBBLE_ROTATION_DEGREES = 45f;

    private final Map<UUID, Double> storedDamage = new HashMap<>();
    private final Map<UUID, Long> chargingPlayers = new HashMap<>();
    private final Map<UUID, Display> amethystBubbles = new HashMap<>();
    private final Map<UUID, BukkitRunnable> bubbleTasks = new HashMap<>();

    public AmethystAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        if (args.length != 2 || !args[0].equalsIgnoreCase("amethyst") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability amethyst 1", NamedTextColor.YELLOW));
            return true;
        }

        if (PlayerClassManager.getClass(player.getUniqueId()) != OreType.AMETHYST) {
            player.sendMessage(Component.text("You are not an Amethyst user!", NamedTextColor.RED));
            return true;
        }

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownSeconds, cooldownSeconds, "amethyst_primary")) {
            player.sendMessage(Component.text("Ability is on cooldown!", NamedTextColor.RED));
            return true;
        }

        UUID id = player.getUniqueId();
        storedDamage.put(id, 0.0);
        chargingPlayers.put(id, System.currentTimeMillis() + (chargeSeconds * 1000L));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                chargeSeconds * 100,
                1,
                false,
                false,
                true
        ));

        player.sendMessage(Component.text("💜 Amethyst Shell activated!", NamedTextColor.LIGHT_PURPLE));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f);
        runParticlesCommand("ppo " + player.getName() + " add dust_color_transition cube purple purple");

        spawnAmethystBubble(player);
        startCooldownTimer(player, cooldownSeconds);

        new BukkitRunnable() {
            @Override
            public void run() {
                releaseBlast(player);
            }
        }.runTaskLater(plugin, chargeSeconds * 20L);

        return true;
    }


    //damage / kb
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!chargingPlayers.containsKey(player.getUniqueId())) return;

        // Store damage
        double stored = event.getFinalDamage() * storedDamagePercent;
        storedDamage.put(player.getUniqueId(),
                storedDamage.get(player.getUniqueId()) + stored);

        // Apply reduced knockback
        Bukkit.getScheduler().runTask(plugin, () -> {
            Vector v = player.getVelocity();
            player.setVelocity(v.multiply(knockbackReduction));
        });
    }

    // item display
    private void spawnAmethystBubble(Player player) {

        Vector center = player.getBoundingBox().getCenter();

        Location loc = new Location(
                player.getWorld(),
                center.getX(),
                center.getY(),
                center.getZ(),
                0f,
                0f
        );

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class);

        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS);
        display.setItemStack(item);

        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(Display.Billboard.FIXED);

        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);
        display.setTeleportDuration(0);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);

        float scale = 3.5f;

        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(
                        (float) Math.toRadians(45),
                        0f, 0f, 1f
                ),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()
        ));

        amethystBubbles.put(player.getUniqueId(), display);

        BukkitRunnable follow = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !chargingPlayers.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                Vector c = player.getBoundingBox().getCenter();
                display.teleport(new Location(
                        player.getWorld(),
                        c.getX(),
                        c.getY(),
                        c.getZ(),
                        0f,
                        0f
                ));
            }
        };

        follow.runTaskTimer(plugin, 0L, 1L);
        bubbleTasks.put(player.getUniqueId(), follow);
    }

    private void shatterBubble(Player player) {
        UUID id = player.getUniqueId();
        Display display = amethystBubbles.remove(id);
        BukkitRunnable follow = bubbleTasks.remove(id);

        if (follow != null) follow.cancel();
        if (display == null) return;

        Location loc = display.getLocation();

        loc.getWorld().spawnParticle(
                Particle.BLOCK_CRUMBLE,
                loc,
                20,
                0.6, 0.8, 0.6,
                Material.PURPLE_STAINED_GLASS.createBlockData()
        );

        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
        display.remove();
    }

    // release blast
    private void releaseBlast(Player caster) {
        runParticlesCommand("ppo " + caster.getName() + " remove dust_color_transition");

        UUID id = caster.getUniqueId();
        chargingPlayers.remove(id);
        caster.removePotionEffect(PotionEffectType.RESISTANCE);

        shatterBubble(caster);

        double stored = storedDamage.getOrDefault(id, 0.0);
        storedDamage.remove(id);

        double blastDamage = Math.min(stored * damageMultiplier, maxBlastDamage);

        Location center = caster.getLocation();
        World world = center.getWorld();

        // Explosion visuals and sound
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.9f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);

        for (LivingEntity target : world.getLivingEntities()) {
            if (target.equals(caster)) continue;
            if (target.getLocation().distanceSquared(center) > blastRadius * blastRadius) continue;

            // === Tick damage for animation/effects ===
            target.setNoDamageTicks(0);
            target.damage(0.1);

            // === True damage applied separately ===
            double trueDamage = blastDamage;
            target.setHealth(Math.max(0, target.getHealth() - trueDamage));

            // Knockback
            Vector kb = target.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(0.5);
            kb.setY(0.15);
            target.setVelocity(kb);
        }
    }

    private void runParticlesCommand(String command) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerParticles"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void startCooldownTimer(Player player, int seconds) {
        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                if (time-- <= 0) {
                    player.sendActionBar(Component.text("✨ Amethyst ready!", NamedTextColor.LIGHT_PURPLE));
                    cancel();
                } else {
                    player.sendActionBar(Component.text("⏱ Amethyst Cooldown: " + time + "s", NamedTextColor.AQUA));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}