package com.gaurav.chronoanchors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ChronoAnchors extends JavaPlugin {

    // Set to store the locations of all active Chrono Anchors.
    private final Set<Location> activeAnchors = new HashSet<>();

    // Configuration variables, loaded from config.yml.
    private Material anchorBlockType;
    private Material activationItemType;
    private double effectRadius;
    private int effectDurationTicks;
    private int effectAmplifier;
    private PotionEffectType distortionEffectType;
    private boolean enableParticles;
    private Particle particleType;
    private int particleCount;

    // ID of the repeating task for time distortion effects.
    private int timeDistortionTaskId = -1;

    @Override
    public void onEnable() {
        getLogger().info("ChronoAnchors plugin enabled!");

        // Save default config.yml if it doesn't exist.
        saveDefaultConfig();
        // Load configuration values.
        loadConfig();

        // Register the event listener.
        getServer().getPluginManager().registerEvents(new AnchorListener(this), this);

        // Start the repeating task that applies time distortion effects.
        startDistortionTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("ChronoAnchors plugin disabled!");

        // Cancel the repeating task to prevent memory leaks.
        if (timeDistortionTaskId != -1) {
            Bukkit.getScheduler().cancelTask(timeDistortionTaskId);
        }

        // Clear the set of active anchors.
        activeAnchors.clear();
    }

    // --- Command Handling ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command being executed is "chronoanchor" (or its aliases).
        if (command.getName().equalsIgnoreCase("chronoanchor")) {
            // Check if there are arguments provided (e.g., "/chronoanchor reload").
            if (args.length > 0) {
                // Check if the first argument is "reload".
                if (args[0].equalsIgnoreCase("reload")) {
                    // Check if the sender has the required permission.
                    if (sender.hasPermission("chronoanchors.command.reload")) {
                        // Call the method to reload the configuration.
                        reloadPluginConfig();
                        // Send a success message to the sender.
                        sender.sendMessage("§aChronoAnchors configuration reloaded successfully!");
                    } else {
                        // Send a no-permission message (defined in plugin.yml).
                        sender.sendMessage(command.getPermissionMessage());
                    }
                    return true; // Command handled.
                }
            }
            // If no arguments or an unknown argument, send usage message.
            sender.sendMessage("§eUsage: /chronoanchor reload");
            return true; // Command handled.
        }
        return false; // Command not handled by this plugin (shouldn't happen if registered correctly).
    }

    /**
     * Reloads the plugin's configuration from config.yml.
     * This method is public so it can be called by the command handler.
     */
    public void reloadPluginConfig() {
        // Reloads the configuration from the disk.
        reloadConfig();
        // Re-load the configuration values into the plugin's variables.
        loadConfig();
        // Restart the distortion task to apply new effect settings immediately.
        restartDistortionTask();
    }

    /**
     * Safely stops and restarts the time distortion task.
     * This is called after config reload to apply new duration, radius, etc.
     */
    private void restartDistortionTask() {
        // First, cancel the existing task if it's running.
        if (timeDistortionTaskId != -1) {
            Bukkit.getScheduler().cancelTask(timeDistortionTaskId);
            timeDistortionTaskId = -1; // Reset task ID
        }
        // Then, start a new task with the potentially new configuration values.
        startDistortionTask();
    }


    /**
     * Loads configuration values from the plugin's config.yml.
     * Handles potential errors if config values are missing or invalid.
     */
    private void loadConfig() {
        FileConfiguration config = getConfig();

        try {
            String anchorBlockTypeName = config.getString("anchor-block-type", "BEACON");
            anchorBlockType = Material.valueOf(anchorBlockTypeName.toUpperCase());

            String activationItemTypeName = config.getString("activation-item-type", "CLOCK");
            activationItemType = Material.valueOf(activationItemTypeName.toUpperCase());

            effectRadius = config.getDouble("effect-radius", 8.0);

            effectDurationTicks = config.getInt("effect-duration-ticks", 40);
            if (effectDurationTicks < 1) {
                getLogger().warning("effect-duration-ticks in config.yml must be at least 1. Setting to default (40).");
                effectDurationTicks = 40;
            }

            effectAmplifier = config.getInt("effect-amplifier", 1);
            if (effectAmplifier < 0) {
                getLogger().warning("effect-amplifier in config.yml cannot be negative. Setting to default (1).");
                effectAmplifier = 1;
            }

            String distortionTypeString = config.getString("distortion-type", "SLOW").toUpperCase();
            if (distortionTypeString.equals("SLOW")) {
                distortionEffectType = PotionEffectType.SLOWNESS;
            } else if (distortionTypeString.equals("FAST")) {
                distortionEffectType = PotionEffectType.SPEED;
            } else {
                getLogger().warning("Invalid distortion-type in config.yml: '" + distortionTypeString + "'. Must be 'SLOW' or 'FAST'. Defaulting to SLOW.");
                distortionEffectType = PotionEffectType.SLOWNESS;
            }

            enableParticles = config.getBoolean("enable-particles", true);
            String particleTypeName = config.getString("particle-type", "PORTAL");
            try {
                particleType = Particle.valueOf(particleTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid particle-type in config.yml: '" + particleTypeName + "'. Defaulting to PORTAL.");
                particleType = Particle.PORTAL;
            }
            particleCount = config.getInt("particle-count", 10);
            if (particleCount < 0) {
                getLogger().warning("particle-count in config.yml cannot be negative. Setting to default (10).");
                particleCount = 10;
            }

            getLogger().info("Configuration loaded successfully.");

        } catch (IllegalArgumentException e) {
            getLogger().log(Level.SEVERE, "Error loading configuration: Invalid material or particle type specified. Please check config.yml.", e);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An unexpected error occurred while loading configuration.", e);
        }
    }

    /**
     * Starts the repeating task that applies time distortion effects.
     * This task iterates through all active anchors and affects nearby entities.
     */
    private void startDistortionTask() {
        timeDistortionTaskId = new TimeDistortionTask(this).runTaskTimer(this, 0L, effectDurationTicks).getTaskId();
    }

    /**
     * Adds a location to the set of active Chrono Anchors.
     *
     * @param location The Location of the Chrono Anchor block.
     */
    public void addActiveAnchor(Location location) {
        activeAnchors.add(location);
        getLogger().info("Chrono Anchor activated at: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
    }

    /**
     * Removes a location from the set of active Chrono Anchors.
     *
     * @param location The Location of the Chrono Anchor block.
     */
    public void removeActiveAnchor(Location location) {
        if (activeAnchors.remove(location)) {
            getLogger().info("Chrono Anchor deactivated at: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        }
    }

    /**
     * Checks if a location is an active Chrono Anchor.
     *
     * @param location The Location to check.
     * @return true if the location is an active anchor, false otherwise.
     */
    public boolean isActiveAnchor(Location location) {
        return activeAnchors.contains(location);
    }

    // --- Getter methods for configuration values ---
    public Material getAnchorBlockType() {
        return anchorBlockType;
    }

    public Material getActivationItemType() {
        return activationItemType;
    }

    public double getEffectRadius() {
        return effectRadius;
    }

    public int getEffectDurationTicks() {
        return effectDurationTicks;
    }

    public int getEffectAmplifier() {
        return effectAmplifier;
    }

    public PotionEffectType getDistortionEffectType() {
        return distortionEffectType;
    }

    public boolean isEnableParticles() {
        return enableParticles;
    }

    public Particle getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    /**
     * Returns the set of all active Chrono Anchor locations.
     * This is used by the TimeDistortionTask to iterate through anchors.
     *
     * @return A Set of Location objects representing active anchors.
     */
    public Set<Location> getActiveAnchors() {
        return activeAnchors;
    }
}
