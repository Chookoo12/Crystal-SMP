package me.Chookoo.testPlugin.utils.items;

import me.Chookoo.testPlugin.utils.roll.RollCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class RerollListener implements Listener {

    private final JavaPlugin plugin;

    public RerollListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {

        // Only main hand
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Only right-click actions
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
            return;

        ItemStack item = event.getItem();
        if (!RerollToken.isToken(item)) return;

        event.setCancelled(true);

        // Remove 1 token
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);

        // Reroll the player
        RollCommand.rollPlayer(plugin, event.getPlayer());

        event.getPlayer().sendMessage("§dYour ability has been rerolled!");
    }
}
