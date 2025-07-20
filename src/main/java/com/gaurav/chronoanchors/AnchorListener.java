package com.gaurav.chronoanchors; // IMPORTANT: Ensure this matches your package structure

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AnchorListener implements Listener {

    private final com.gaurav.chronoanchors.ChronoAnchors plugin;

    // Constructor to get an instance of the main plugin class.
    public AnchorListener(com.gaurav.chronoanchors.ChronoAnchors plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player interactions (e.g., right-clicking blocks).
     * This method is responsible for activating/deactivating Chrono Anchors.
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ensure the interaction is a right-click on a block.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ensure the interaction is not off-hand to prevent double triggers.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if the clicked block is the configured anchor block type.
        if (clickedBlock != null && clickedBlock.getType() == plugin.getAnchorBlockType()) {
            Location anchorLocation = clickedBlock.getLocation();

            // Check if the player is holding the configured activation item.
            if (itemInHand.getType() == plugin.getActivationItemType()) {
                event.setCancelled(true); // Prevent default item action (e.g., placing the item)

                if (plugin.isActiveAnchor(anchorLocation)) {
                    // If it's already an active anchor, deactivate it.
                    plugin.removeActiveAnchor(anchorLocation);
                    player.sendMessage("§eChrono Anchor deactivated!");
                } else {
                    // If it's not an active anchor, activate it.
                    plugin.addActiveAnchor(anchorLocation);
                    player.sendMessage("§aChrono Anchor activated!");
                }
            }
        }
    }

    /**
     * Handles block breaking events.
     * This method is responsible for deactivating Chrono Anchors if they are broken.
     *
     * @param event The BlockBreakEvent.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Location anchorLocation = brokenBlock.getLocation();

        // If the broken block is an active Chrono Anchor, remove it from the active list.
        if (brokenBlock.getType() == plugin.getAnchorBlockType() && plugin.isActiveAnchor(anchorLocation)) {
            plugin.removeActiveAnchor(anchorLocation);
            event.getPlayer().sendMessage("§cChrono Anchor destroyed!");
        }
    }
}
