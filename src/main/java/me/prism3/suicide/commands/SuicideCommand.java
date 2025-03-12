package me.prism3.suicide.commands;

import me.prism3.suicide.Suicide;
import me.prism3.suicide.utils.Data;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Handles execution of the suicide command and related functionalities.
 * Manages player cooldowns, command permissions, and post-suicide effects.
 *
 * @author Prism3
 * @since 1.0
 */
public class SuicideCommand implements CommandExecutor {

    /**
     * Reference to the main plugin instance
     */
    private final Suicide plugin;

    /**
     * Configuration data manager instance
     */
    private final Data data;

    /**
     * Cooldown tracker storing player UUIDs and their cooldown end timestamps
     * Key: Player UUID
     * Value: Cooldown expiration time in milliseconds
     */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private static final String COORD_FORMAT = "&fYou suicided at: &cX: %d Y: %d Z: %d";


    /**
     * Initializes a new SuicideCommand instance
     *
     * @param data Configuration data manager instance
     */
    public SuicideCommand(final Data data) {
        this.plugin = Suicide.getInstance();
        this.data = data;
    }

    /**
     * Executes the suicide command and handles all related logic
     *
     * @param sender The command sender
     * @param cmd    The command being executed
     * @param label  The alias used
     * @param args   Command arguments
     * @return true if command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        // Handle permission check
        if (!sender.hasPermission(this.data.getSuicideCommand())) {
            this.sendPermissionMessage(sender);
            return true;
        }

        // Handle reload subcommand
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return this.handleReload(sender);
        }

        // Validate command syntax
        if (args.length != 0) {
            this.sendInvalidSyntaxMessage(sender);
            return true;
        }

        // Verify sender is a player
        if (!(sender instanceof Player player)) {
            this.handleNonPlayerExecution();
            return true;
        }

        // Check disabled worlds
        if (this.isInDisabledWorld(player)) {
            this.sendDisabledWorldMessage(player);
            return true;
        }

        // Process cooldown checks
        if (this.isOnCooldown(player))
            return true;

        // Apply cooldown if enabled
        this.applyCooldown(player);

        // Execute suicide sequence
        this.executeSuicideSequence(player);

        return true;
    }

    /**
     * Handles the reload subcommand execution
     *
     * @param sender The command sender
     * @return true if reload was successful, false otherwise
     */
    private boolean handleReload(final CommandSender sender) {

        if (!sender.hasPermission(this.data.getSuicideReload())) {
            this.sendPermissionMessage(sender);
            return true;
        }

        this.plugin.reloadConfig();
        this.data.load();
        sender.sendMessage(colorize(this.data.getReloadMessage()));

        return true;
    }

    /**
     * Checks if player is in a disabled world
     *
     * @param player The player to check
     * @return true if world is disabled, false otherwise
     */
    private boolean isInDisabledWorld(final Player player) {
        return this.data.getDisabledWorlds().contains(player.getWorld().getName());
    }

    /**
     * Checks and manages player cooldown status using atomic operations.
     * Automatically cleans expired cooldowns during check.
     *
     * @param player The player to check
     * @return true if player is currently on cooldown
     */
    private boolean isOnCooldown(final Player player) {

        if (!data.isCooldownEnabled() || player.hasPermission(data.getSuicideBypass()))
            return false;

        final UUID playerId = player.getUniqueId();
        final Long cooldownEnd = cooldowns.get(playerId);

        if (cooldownEnd == null)
            return false;

        final long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;

        if (remaining > 0) {
            this.sendCooldownMessage(player, remaining);
            return true;
        }

        // Atomic removal of expired cooldown
        cooldowns.remove(playerId, cooldownEnd);
        return false;
    }

    /**
     * Applies cooldown using computeIfPresent for atomic updates
     *
     * @param player The player to apply cooldown to
     */
    private void applyCooldown(final Player player) {

        if (!data.isCooldownEnabled() || player.hasPermission(data.getSuicideBypass()))
            return;

        final UUID playerId = player.getUniqueId();
        final long newCooldownEnd = System.currentTimeMillis() + (data.getCoolDownTime() * 1000);

        cooldowns.compute(playerId, (uuid, currentEnd) ->
                (currentEnd == null || newCooldownEnd > currentEnd) ? newCooldownEnd : currentEnd
        );

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                        cooldowns.remove(playerId, newCooldownEnd),
                data.getCoolDownTime() * 20L
        );
    }

    /**
     * Executes full suicide sequence for player
     *
     * @param player The player executing the command
     */
    private void executeSuicideSequence(final Player player) {
        // Track player and kill
        this.plugin.getPlayers().add(player.getUniqueId());
        player.setHealth(0.0);

        // Execute post-suicide effects
        this.executePostSuicideEffects(player);
    }

    /**
     * Executes all configured post-suicide effects
     *
     * @param player The player who executed the command
     */
    private void executePostSuicideEffects(final Player player) {
        if (this.data.isBroadcastEnabled()) this.broadcast(player);
        if (this.data.isMessageEnabled()) this.sendSuicideMessage(player);
        if (this.data.isFireworkEnabled()) this.spawnFirework(player.getLocation());
        if (this.data.isCoordsEnabled()) this.displayCoords(player);
        if (this.data.isSoundEnabled()) this.playSound(player);
    }

    /**
     * Sends permission error message to sender
     *
     * @param sender The command sender
     */
    private void sendPermissionMessage(final CommandSender sender) {
        sender.sendMessage(colorize(this.data.getNoPermissionMessage()));
    }

    /**
     * Sends invalid syntax message to sender
     *
     * @param sender The command sender
     */
    private void sendInvalidSyntaxMessage(final CommandSender sender) {
        sender.sendMessage(colorize(this.data.getInvalidSyntaxMessage()));
    }

    /**
     * Handles non-player command execution attempts
     */
    private void handleNonPlayerExecution() {
        this.plugin.getLogger().severe("This command can only be run in-game!");
    }

    /**
     * Sends disabled world message to player
     *
     * @param player The affected player
     */
    private void sendDisabledWorldMessage(final Player player) {
        player.sendMessage(colorize(this.data.getDisabledWorldMessage()));
    }

    /**
     * Sends cooldown message to player
     *
     * @param player The affected player
     * @param seconds Remaining cooldown time in seconds
     */
    private void sendCooldownMessage(final Player player, final long seconds) {
        final String message = this.data.getCoolDownMessage()
                .replace("%time%", String.valueOf(seconds));
        player.sendMessage(colorize(message));
    }

    /**
     * Sends suicide confirmation message to player
     *
     * @param player The affected player
     */
    private void sendSuicideMessage(final Player player) {
        player.sendMessage(colorize(this.data.getSuicideMessage()));
    }

    /**
     * Broadcasts suicide announcement using efficient random selection
     * - Uses ThreadLocalRandom for thread-safe randomization
     * - Avoids full list shuffling for better performance
     *
     * @param player The player who executed the command
     */
    private void broadcast(final Player player) {

        final List<String> messages = this.data.getBroadcastMessages();

        if (messages.isEmpty()) return;

        final int index = ThreadLocalRandom.current().nextInt(messages.size());
        final String raw = messages.get(index)
                .replace("%player%", player.getName());

        Bukkit.broadcastMessage(colorize(raw));
    }

    /**
     * Spawns configured firework effect at location with safety checks
     *
     * @param loc The location to spawn the firework
     */
    private void spawnFirework(final Location loc) {

        if (loc.getWorld() == null)
            return;

        final Firework fw = loc.getWorld().spawn(loc, Firework.class);

        try {
            final FireworkMeta meta = fw.getFireworkMeta();
            final FireworkEffect effect = FireworkEffect.builder()
                    .with(Type.valueOf(data.getFireworkType()))
                    .withColor(this.createColor(
                            data.getFireworkColorRed(),
                            data.getFireworkColorGreen(),
                            data.getFireworkColorBlue()
                    ))
                    .withFade(this.createColor(
                            data.getFireworkFadeColorRed(),
                            data.getFireworkFadeColorGreen(),
                            data.getFireworkFadeColorBlue()
                    ))
                    .trail(data.isFireworkTrail())
                    .flicker(data.isFireworkFlicker())
                    .build();

            meta.addEffect(effect);
            meta.setPower(Math.min(3, Math.max(0, data.getFireworkPower())));
            fw.setFireworkMeta(meta);
            fw.setMetadata("noDamage", new FixedMetadataValue(plugin, true));
        } catch (final IllegalArgumentException e) {
            this.plugin.getLogger().warning("Invalid firework config: " + e.getMessage());
            fw.remove(); // Remove invalid firework to prevent visual glitch
        }
    }

    /**
     * Creates a color from RGB values
     *
     * @param r The red value
     * @param g The green value
     * @param b The blue value
     * @return The created color
     */
    private Color createColor(final int r, final int g, final int b) {
        return Color.fromRGB(
                this.clamp(r),
                this.clamp(g),
                this.clamp(b)
        );
    }

    /**
     * Clamps a value between 0 and 255
     *
     * @param value The value to clamp
     * @return The clamped value
     */
    private int clamp(final int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Displays suicide coordinates to player
     *
     * @param player The player to display coordinates to
     */
    private void displayCoords(final Player player) {
        final Location loc = player.getLocation();
        player.sendMessage(colorize(
                String.format(COORD_FORMAT,
                        loc.getBlockX(),
                        loc.getBlockY(),
                        loc.getBlockZ()
                )
        ));
    }

    /**
     * Plays configured sound effect for player
     *
     * @param player The player to play sound for
     */
    private void playSound(final Player player) {

        if (!this.data.isSoundEnabled()) return;

        final String rawSound = this.data.getPlayedSound();
        final boolean isLegacy = Bukkit.getServer().getClass().getPackage().getName().contains("v1_12");

        // Convert to legacy format if needed
        String soundName = rawSound.toUpperCase().replace("ENTITY_", "MOB_");
        if (isLegacy) {
            soundName = soundName
                    .replace("ITEM_", "")
                    .replace("BLOCK_", "")
                    .replace("ENTITY_", "MOB_");
        }

        final float volume = this.data.getSoundVolume() / 100f;
        final float pitch = 0.5f + (this.data.getSoundPitch() / 100f * 1.5f);

        try {
            // Use string-based sound method
            player.playSound(player.getLocation(), soundName, volume, pitch);
        } catch (final IllegalArgumentException e) {
            this.plugin.getLogger().severe("Invalid sound '" + rawSound + "'");
            this.plugin.getLogger().severe("Try " + (isLegacy ? "MOB_ZOMBIE_HURT" : "ENTITY_ZOMBIE_HURT"));
        }
    }

    /**
     * Colorizes text with color codes
     *
     * @param text The text to colorize
     * @return The colorized text
     */
    private static String colorize(final String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
