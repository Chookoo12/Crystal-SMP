package me.Chookoo.testPlugin.utils.abilities;

import me.Chookoo.testPlugin.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class GoldAbility implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;
    private final Random random = new Random();

    // ===== Weighted chances for each symbol (higher = more likely) =====
    private final Map<SlotSymbol, Integer> symbolWeights = Map.of(
            SlotSymbol.POWER, 25,
            SlotSymbol.GOLD, 25,
            SlotSymbol.CURSE, 40,
            SlotSymbol.JACKPOT, 10
    );

    public GoldAbility(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private enum SlotSymbol {
        POWER(Material.BLAZE_POWDER, "⚡ POWER", NamedTextColor.LIGHT_PURPLE),
        GOLD(Material.GOLD_INGOT, "💰 WEALTH", NamedTextColor.GOLD),
        CURSE(Material.WITHER_ROSE, "☠ CURSE", NamedTextColor.GREEN),
        JACKPOT(Material.DIAMOND, "💎 JACKPOT", NamedTextColor.AQUA);

        final Material material;
        final String name;
        final NamedTextColor color;

        SlotSymbol(Material material, String name, NamedTextColor color) {
            this.material = material;
            this.name = name;
            this.color = color;
        }

        ItemStack toItem() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(name, color));
            item.setItemMeta(meta);
            return item;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("gold") || !args[1].equals("1")) {
            player.sendMessage(Component.text("Usage: /ability gold 1", NamedTextColor.YELLOW));
            return true;
        }

        if (!cooldownManager.tryUseAbilityCooldownOnly(player, 10, 30, "gold")) return true;

        // Start action-bar cooldown indicator
        int remaining = cooldownManager.getPlayerCooldown(player.getUniqueId(), "gold");
        startCooldownIndicator(player, remaining, "Gold Ability");

        SlotSymbol finalSymbol = randomSymbol();
        openSlotMachine(player, finalSymbol);
        return true;
    }

    private void openSlotMachine(Player player, SlotSymbol finalSymbol) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("🎰 Gilded Slots", NamedTextColor.GOLD));
        player.openInventory(inv);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false, true));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                for (int i = 3; i <= 5; i++) inv.setItem(i, randomSymbol().toItem());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.8f);

                if (ticks >= 14) {
                    cancel();
                    flashFinalSymbol(player, inv, finalSymbol);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void flashFinalSymbol(Player player, Inventory inv, SlotSymbol finalSymbol) {
        new BukkitRunnable() {
            int flashes = 0;
            @Override
            public void run() {
                flashes++;
                for (int i = 3; i <= 5; i++) {
                    if (flashes % 2 == 0) inv.setItem(i, finalSymbol.toItem());
                    else inv.setItem(i, randomSymbol().toItem());
                }
                if (flashes >= 6) {
                    for (int i = 3; i <= 5; i++) inv.setItem(i, finalSymbol.toItem());
                    applyEffect(player, finalSymbol);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void applyEffect(Player player, SlotSymbol symbol) {
        switch (symbol) {
            case POWER -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 13, 2));
            case GOLD -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 10, 1));
                player.giveExp(80);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (LivingEntity target : player.getNearbyEntities(5, 5, 5).stream()
                                .filter(e -> e instanceof LivingEntity)
                                .map(e -> (LivingEntity) e)
                                .toList()) {

                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 100, false, false, true));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 2, false, false, true));
                        }

                        player.getWorld().spawnParticle(Particle.WHITE_SMOKE, player.getLocation().add(0,1,0), 50, 1,1,1, 0.05);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    }
                }.runTaskLater(plugin, 14L);

            }
            case CURSE -> {
                if (!player.getActivePotionEffects().isEmpty()) {
                    List<PotionEffect> effects = List.copyOf(player.getActivePotionEffects());
                    PotionEffect removed = effects.get(random.nextInt(effects.size()));
                    player.removePotionEffect(removed.getType());
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 1.4f);
                }
            }
            case JACKPOT -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 15, 2));
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.getWorld().getNearbyPlayers(player.getLocation(), 50)
                        .forEach(p -> p.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f));
            }
        }
        player.sendMessage(Component.text("🎰 You got: " + symbol.name(), NamedTextColor.YELLOW));
    }

    // ===== Weighted random selection =====
    private SlotSymbol randomSymbol() {
        int totalWeight = symbolWeights.values().stream().mapToInt(Integer::intValue).sum();
        int r = random.nextInt(totalWeight);
        int cumulative = 0;

        for (SlotSymbol symbol : SlotSymbol.values()) {
            cumulative += symbolWeights.get(symbol);
            if (r < cumulative) return symbol;
        }

        return SlotSymbol.POWER; // fallback
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text("🎰 Gilded Slots", NamedTextColor.GOLD))) {
            event.setCancelled(true);
        }
    }

    private void startCooldownIndicator(Player player, int cooldownSeconds, String abilityName) {
        new BukkitRunnable() {
            int remaining = cooldownSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    player.sendActionBar(Component.text("✨ " + abilityName + " ready!", NamedTextColor.YELLOW));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text("⏳ " + abilityName + " cooldown: " + remaining + "s", NamedTextColor.RED));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
