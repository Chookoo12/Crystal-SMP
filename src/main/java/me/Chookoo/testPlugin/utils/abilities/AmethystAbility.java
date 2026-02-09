package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AmethystAbility implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    // ---------- Adjustable Parameters ----------
    private final int cooldownSeconds = 20;
    private final int chargeSeconds = 2;
    private final double storedDamagePercent = 0.45;
    private final double damageMultiplier = 1.0;
    private final double blastRadius = 3.5;
    private final double maxBlastDamage = 14.0;
    private final double knockbackReduction = 0.35;
    // -------------------------------------------

    private final Map<UUID, Double> storedDamage = new HashMap<>();
    private final Map<UUID, Long> chargingPlayers = new HashMap<>();

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

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, cooldownSeconds, cooldownSeconds, "amethyst"))
            return true;

        UUID id = player.getUniqueId();
        storedDamage.put(id, 0.0);
        chargingPlayers.put(id, System.currentTimeMillis() + (chargeSeconds * 1000L));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                chargeSeconds * 40,
                9999,
                false,
                false,
                true
        ));

        player.sendMessage(Component.text("💜 Amethyst Shell activated!", NamedTextColor.LIGHT_PURPLE));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f);

        runParticlesCommand("ppo " + player.getName() + " add dust_color_transition cube purple purple");

        startCooldownTimer(player, cooldownSeconds);

        new BukkitRunnable() {
            @Override
            public void run() {
                releaseBlast(player);
            }
        }.runTaskLater(plugin, chargeSeconds * 20L);

        return true;
    }

    // damage
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        if (!chargingPlayers.containsKey(id)) return;

        double rawDamage = event.getFinalDamage();
        double stored = rawDamage * storedDamagePercent;
        storedDamage.put(id, storedDamage.get(id) + stored);

        // reduce kb
        player.setVelocity(player.getVelocity().multiply(knockbackReduction));

        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 1, 0),
                6,
                0.4,
                0.6,
                0.4,
                new Particle.DustOptions(Color.fromRGB(190, 120, 255), 1.3f)
        );
    }

    // anti jump
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!chargingPlayers.containsKey(id)) return;

        Location loc = player.getLocation();
        // Get the block directly under the player
        Block under = loc.getBlock().getRelative(BlockFace.DOWN);

        // If standing on air (fell slightly), teleport them back to the ground
        if (under.getType().isAir()) return; // let them fall naturally if not on solid ground

        double groundY = under.getY() + 1.0; // top of the block
        if (loc.getY() > groundY) {
            loc.setY(groundY);
            player.teleport(loc);
        }

        // Optional: make it feel smooth
        player.setVelocity(new Vector(player.getVelocity().getX(), 0, player.getVelocity().getZ()));
    }



    // release
    private void releaseBlast(Player caster) {

        runParticlesCommand("ppo " + caster.getName() + " remove dust_color_transition");

        UUID id = caster.getUniqueId();

        chargingPlayers.remove(id);

        caster.removePotionEffect(PotionEffectType.SLOWNESS);

        double stored = storedDamage.remove(id);

        double blastDamage = Math.min(stored * damageMultiplier, maxBlastDamage);
        Location center = caster.getLocation();

        caster.sendMessage(Component.text("💥 Amethyst Overload!", NamedTextColor.DARK_PURPLE));
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.9f);

        center.getWorld().spawnParticle(
                Particle.EXPLOSION_EMITTER,
                center,
                1
        );

        for (LivingEntity target : center.getWorld().getLivingEntities()) {
            if (target.equals(caster)) continue;
            if (target.getLocation().distanceSquared(center) > blastRadius * blastRadius) continue;

            // True damage (NO death spam)
            target.damage(0.1, caster);
            target.setHealth(Math.max(0, target.getHealth() - blastDamage));

            double distance = target.getLocation().distance(center);
            double scale = Math.max(0.2, 1.0 - (distance / blastRadius));

            Vector kb = target.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(0.5 * scale);

            kb.setY(0.15 * scale);
            target.setVelocity(kb);
        }
    }

    private void runParticlesCommand(String command) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerParticles")) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    // cd
    private void startCooldownTimer(Player player, int seconds) {
        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                if (time <= 0) {
                    player.sendActionBar(Component.text("✨ Amethyst ready!", NamedTextColor.LIGHT_PURPLE));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text("⏱ Amethyst Cooldown: " + time + "s", NamedTextColor.AQUA));
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
