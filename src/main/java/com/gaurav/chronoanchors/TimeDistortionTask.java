package com.gaurav.chronoanchors; // IMPORTANT: Ensure this matches your package structure

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class TimeDistortionTask extends BukkitRunnable {

    private final com.gaurav.chronoanchors.ChronoAnchors plugin;

    // Constructor to get an instance of the main plugin class.
    public TimeDistortionTask(com.gaurav.chronoanchors.ChronoAnchors plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Iterate through all active Chrono Anchor locations.
        for (Location anchorLocation : plugin.getActiveAnchors()) {
            // Ensure the anchor block still exists at the location.
            if (anchorLocation.getBlock().getType() != plugin.getAnchorBlockType()) {
                // If the block is no longer the anchor type, remove it from active anchors.
                plugin.removeActiveAnchor(anchorLocation);
                continue; // Skip to the next anchor
            }

            World world = anchorLocation.getWorld();
            if (world == null) {
                plugin.getLogger().warning("Active anchor at " + anchorLocation + " has no associated world. Skipping.");
                continue;
            }

            // Apply time distortion effects to entities within the radius.
            applyDistortion(anchorLocation, world);

            // Spawn particle effects if enabled.
            if (plugin.isEnableParticles()) {
                spawnParticles(anchorLocation, world);
            }
        }
    }

    /**
     * Applies the configured time distortion potion effect to entities within the anchor's radius.
     *
     * @param anchorLocation The location of the Chrono Anchor.
     * @param world          The world the anchor is in.
     */
    private void applyDistortion(Location anchorLocation, World world) {
        // Get all entities within the specified radius around the anchor.
        for (Entity entity : world.getNearbyEntities(anchorLocation,
                plugin.getEffectRadius(), plugin.getEffectRadius(), plugin.getEffectRadius())) {

            // We only want to affect LivingEntities (players, mobs).
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                // Create a new PotionEffect with the configured type, duration, and amplifier.
                PotionEffect effect = new PotionEffect(
                        plugin.getDistortionEffectType(),
                        plugin.getEffectDurationTicks(),
                        plugin.getEffectAmplifier(),
                        true, // ambient
                        false // particles
                );

                // Apply the potion effect.
                livingEntity.addPotionEffect(effect);
            }
        }
    }

    /**
     * Spawns particle effects around the Chrono Anchor.
     *
     * @param anchorLocation The location of the Chrono Anchor.
     * @param world          The world the anchor is in.
     */
    private void spawnParticles(Location anchorLocation, World world) {
        // Spawn particles at the anchor's location.
        world.spawnParticle(
                plugin.getParticleType(),
                anchorLocation.getX() + 0.5, // Center the particles on the block
                anchorLocation.getY() + 1.0, // Slightly above the block
                anchorLocation.getZ() + 0.5, // Center the particles on the block
                plugin.getParticleCount(),   // Number of particles
                plugin.getEffectRadius() * 0.8, // Offset X (spread of particles)
                plugin.getEffectRadius() * 0.8, // Offset Y
                plugin.getEffectRadius() * 0.8, // Offset Z
                0.01 // Extra data (speed of particles, depends on particle type)
        );
    }
}
