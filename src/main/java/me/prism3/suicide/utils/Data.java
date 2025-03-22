package me.prism3.suicide.utils;

import me.prism3.suicide.Suicide;
import me.prism3.suicide.commands.SuicideCommand;
import me.prism3.suicide.events.EntityDamage;
import me.prism3.suicide.events.PlayerDeath;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Central configuration manager handling all plugin settings and data.
 * Responsible for loading, storing, and providing access to configuration values.
 * Manages dynamic command registration and event setup.
 *
 * @author Prism3
 * @since 1.0
 */
public class Data {

    /**
     * Main plugin instance reference
     */
    private final Suicide plugin;

    // String configurations
    private String suicideReload;
    private String suicideBypass;
    private String suicideCommand;
    private String suicideMessage;
    private String noPermissionMessage;
    private String reloadMessage;
    private String invalidSyntaxMessage;
    private String disabledWorldMessage;
    private String coolDownMessage;
    private String fireworkType;

    // Numerical configurations
    private long coolDownTime;
    private int resourceID;
    private int soundVolume;
    private int soundPitch;
    private int fireworkPower;
    private int fireworkColorRed;
    private int fireworkColorGreen;
    private int fireworkColorBlue;
    private int fireworkFadeColorRed;
    private int fireworkFadeColorGreen;
    private int fireworkFadeColorBlue;

    // Boolean feature toggles
    private boolean cooldownEnabled;
    private boolean broadcastEnabled;
    private boolean messageEnabled;
    private boolean fireworkEnabled;
    private boolean fireworkTrail;
    private boolean fireworkFlicker;
    private boolean coordsEnabled;
    private boolean soundEnabled;

    // List configurations
    private List<String> disabledWorlds;
    private List<String> broadcastMessages;
    private List<String> commandAliases;

    /**
     * Plugin resource ID for update checking
     */
    private static final int DEFAULT_RESOURCE_ID = 93367;

    /**
     * Default permission nodes
     */
    private static final String COMMAND_PERMISSION = "suicide.command";
    private static final String RELOAD_PERMISSION = "suicide.reload";
    private static final String BYPASS_PERMISSION = "suicide.bypass";

    /**
     * Initializes a new Data manager instance
     *
     * @param plugin Main plugin instance
     */
    public Data(final Suicide plugin) {
        this.plugin = plugin;
        this.load();
    }

    /**
     * Loads and refreshes all configuration values from disk
     * Registers plugin components after loading
     */
    public void load() {
        this.initializeConfigFile();
        this.loadStringValues();
        this.loadNumericValues();
        this.loadBooleanToggles();
        this.loadListValues();
        this.registerPluginComponents();
    }

    /**
     * Ensures configuration file exists with default values
     */
    private void initializeConfigFile() {
        this.plugin.saveDefaultConfig();
    }

    /**
     * Loads all string-based configuration values
     */
    private void loadStringValues() {
        this.suicideMessage = this.getConfigStringWithDefault("Messages.On-Suicide");
        this.noPermissionMessage = this.getConfigStringWithDefault("Messages.No-Permission");
        this.reloadMessage = this.getConfigStringWithDefault("Messages.Reload");
        this.invalidSyntaxMessage = this.getConfigStringWithDefault("Messages.Invalid-Syntax");
        this.disabledWorldMessage = this.getConfigStringWithDefault("Messages.Disabled");
        this.coolDownMessage = this.getConfigStringWithDefault("Messages.On-Cooldown");
        this.fireworkType = this.getConfigStringWithDefault("Firework.Type", "BALL_LARGE").toUpperCase();
        this.suicideCommand = COMMAND_PERMISSION;
        this.suicideReload = RELOAD_PERMISSION;
        this.suicideBypass = BYPASS_PERMISSION;
    }

    /**
     * Loads all numeric configuration values
     */
    private void loadNumericValues() {
        this.coolDownTime = this.plugin.getConfig().getLong("Cooldown.Timer");
        this.resourceID = DEFAULT_RESOURCE_ID;
        this.soundVolume = this.plugin.getConfig().getInt("Sound.Volume");
        this.soundPitch = this.plugin.getConfig().getInt("Sound.Pitch");
        this.fireworkColorRed = this.plugin.getConfig().getInt("Firework.Color.RED");
        this.fireworkColorGreen = this.plugin.getConfig().getInt("Firework.Color.GREEN");
        this.fireworkColorBlue = this.plugin.getConfig().getInt("Firework.Color.BLUE");
        this.fireworkFadeColorRed = this.plugin.getConfig().getInt("Firework.Fade.RED");
        this.fireworkFadeColorGreen = this.plugin.getConfig().getInt("Firework.Fade.GREEN");
        this.fireworkFadeColorBlue = this.plugin.getConfig().getInt("Firework.Fade.BLUE");
        this.fireworkPower = this.plugin.getConfig().getInt("Firework.Power");
    }

    /**
     * Loads all boolean feature toggles
     */
    private void loadBooleanToggles() {
        this.messageEnabled = this.plugin.getConfig().getBoolean("Message", true);
        this.broadcastEnabled = this.plugin.getConfig().getBoolean("Broadcast", true);
        this.fireworkEnabled = this.plugin.getConfig().getBoolean("Firework.Enabled", true);
        this.coordsEnabled = this.plugin.getConfig().getBoolean("Coords", true);
        this.soundEnabled = this.plugin.getConfig().getBoolean("Sound.Enabled", true);
        this.cooldownEnabled = this.plugin.getConfig().getBoolean("Cooldown.Enabled", true);

        this.fireworkTrail = this.plugin.getConfig().getBoolean("Firework.Trail", true);
        this.fireworkFlicker = this.plugin.getConfig().getBoolean("Firework.Flicker", true);
    }

    /**
     * Loads all list-based configuration values
     */
    private void loadListValues() {
        this.disabledWorlds = this.plugin.getConfig().getStringList("Disabled-Worlds");
        this.broadcastMessages = this.plugin.getConfig().getStringList("Messages.Broadcast.Messages");
        this.commandAliases = this.plugin.getConfig().getStringList("Aliases");
    }

    /**
     * Registers all plugin components including events and commands
     */
    private void registerPluginComponents() {
        this.registerEventListeners();
        this.registerMainCommand();
    }

    /**
     * Registers all event listeners
     */
    private void registerEventListeners() {
        this.plugin.getServer().getPluginManager().registerEvents(new PlayerDeath(), plugin);
        this.plugin.getServer().getPluginManager().registerEvents(new EntityDamage(), plugin);
    }

    /**
     * Registers and configures the main command with aliases
     */
    private void registerMainCommand() {

        final PluginCommand command = this.plugin.getCommand("suicide");

        if (command == null)
            return;

        this.configureCommandProperties(command);
        this.refreshCommandRegistration(command);
    }

    /**
     * Configures command properties from loaded values
     *
     * @param command The command to configure
     */
    private void configureCommandProperties(final PluginCommand command) {
        command.setAliases(commandAliases);
        command.setExecutor(new SuicideCommand(this));
    }

    /**
     * Refreshes command registration in the server's command map
     *
     * @param command The command to refresh
     */
    private void refreshCommandRegistration(final PluginCommand command) {
        try {
            final SimpleCommandMap commandMap = this.getCommandMap();
            this.unregisterExistingCommand(command, commandMap);
            commandMap.register(this.plugin.getName(), command);
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Command registration error: " + e.getMessage());
        }
    }

    /**
     * Retrieves the server's command map using reflection
     */
    private SimpleCommandMap getCommandMap() throws Exception {
        final Method getCommandMap = this.plugin.getServer().getClass().getMethod("getCommandMap");
        return (SimpleCommandMap) getCommandMap.invoke(this.plugin.getServer());
    }

    /**
     * Unregisters a command from the command map
     *
     * @param command The command to unregister
     * @param commandMap The command map to unregister from
     */
    private void unregisterExistingCommand(final PluginCommand command, final SimpleCommandMap commandMap) {
        // Unregister the command normally.
        command.unregister(commandMap);

        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            // Copy keys into a list to avoid concurrent modification issues.
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
                if (entry.getValue() == command && !entry.getKey().equalsIgnoreCase(command.getName())) {
                    keysToRemove.add(entry.getKey());
                }
            }

            // Remove stale aliases by key.
            for (String key : keysToRemove) {
                knownCommands.remove(key);
            }
        } catch (final Exception e) {
            plugin.getLogger().severe("Error cleaning up old aliases: " + e.getMessage());
            plugin.getLogger().severe("If the issue persists, contact the author");
        }
    }

    /**
     * Safely retrieves a string value from config with optional default
     */
    private String getConfigStringWithDefault(final String path, final String defaultValue) {
        return this.plugin.getConfig().getString(path, defaultValue);
    }

    private String getConfigStringWithDefault(final String path) {
        return this.plugin.getConfig().getString(path);
    }

    /**
     * Retrieves the sound effect played during suicide
     * @return Name of configured sound effect
     */
    public String getPlayedSound() {
        return this.plugin.getConfig().getString("Sound.Sound", "MOB_ZOMBIE_HURT");
    }

    /**
     * Gets the reload subcommand permission node
     * @return Permission string for reload access
     */
    public String getSuicideReload() { return this.suicideReload; }

    /**
     * Gets the cooldown bypass permission node
     * @return Permission string for bypassing cooldowns
     */
    public String getSuicideBypass() { return this.suicideBypass; }

    /**
     * Gets the base command permission node
     * @return Permission string for command access
     */
    public String getSuicideCommand() { return this.suicideCommand; }

    /**
     * Gets the suicide confirmation message
     * @return Configured suicide completion message
     */
    public String getSuicideMessage() { return this.suicideMessage; }

    /**
     * Gets the no-permission error message
     * @return Permission denial message template
     */
    public String getNoPermissionMessage() { return this.noPermissionMessage; }

    /**
     * Gets the config reload confirmation
     * @return Reload success message template
     */
    public String getReloadMessage() { return this.reloadMessage; }

    /**
     * Gets the invalid syntax warning
     * @return Command syntax error message template
     */
    public String getInvalidSyntaxMessage() { return this.invalidSyntaxMessage; }

    /**
     * Gets disabled world error message
     * @return World restriction message template
     */
    public String getDisabledWorldMessage() { return this.disabledWorldMessage; }

    /**
     * Gets cooldown active warning
     * @return Cooldown notification message template
     */
    public String getCoolDownMessage() { return this.coolDownMessage; }

    /**
     * Gets configured firework effect type
     * @return FireworkType name in uppercase
     */
    public String getFireworkType() { return this.fireworkType; }

    /**
     * Gets cooldown duration in seconds
     * @return Cooldown length in seconds
     */
    public long getCoolDownTime() { return this.coolDownTime; }

    /**
     * Gets plugin resource ID for updates
     * @return Spigot resource identifier
     */
    public int getResourceID() { return this.resourceID; }

    /**
     * Gets suicide sound effect volume
     * @return Sound volume level (0-100)
     */
    public int getSoundVolume() { return this.soundVolume; }

    /**
     * Gets suicide sound effect pitch
     * @return Sound pitch value (0-2)
     */
    public int getSoundPitch() { return this.soundPitch; }

    /**
     * Gets firework launch power
     * @return Firework rocket power level
     */
    public int getFireworkPower() { return this.fireworkPower; }

    /**
     * Gets firework primary color red component
     * @return RGB red value (0-255)
     */
    public int getFireworkColorRed() { return this.fireworkColorRed; }

    /**
     * Gets firework primary color green component
     * @return RGB green value (0-255)
     */
    public int getFireworkColorGreen() { return this.fireworkColorGreen; }

    /**
     * Gets firework primary color blue component
     * @return RGB blue value (0-255)
     */
    public int getFireworkColorBlue() { return this.fireworkColorBlue; }

    /**
     * Gets firework fade color red component
     * @return RGB red value (0-255)
     */
    public int getFireworkFadeColorRed() { return this.fireworkFadeColorRed; }

    /**
     * Gets firework fade color green component
     * @return RGB green value (0-255)
     */
    public int getFireworkFadeColorGreen() { return this.fireworkFadeColorGreen; }

    /**
     * Gets firework fade color blue component
     * @return RGB blue value (0-255)
     */
    public int getFireworkFadeColorBlue() { return this.fireworkFadeColorBlue; }

    /**
     * Checks if cooldown system is enabled
     * @return true if cooldowns are active
     */
    public boolean isCooldownEnabled() { return this.cooldownEnabled; }

    /**
     * Checks if broadcast messages are enabled
     * @return true if broadcasts are active
     */
    public boolean isBroadcastEnabled() { return this.broadcastEnabled; }

    /**
     * Checks if personal messages are enabled
     * @return true if player messages are active
     */
    public boolean isMessageEnabled() { return this.messageEnabled; }

    /**
     * Checks if fireworks are enabled
     * @return true if firework effects are active
     */
    public boolean isFireworkEnabled() { return this.fireworkEnabled; }

    /**
     * Checks if firework trails are enabled
     * @return true if firework trails are active
     */
    public boolean isFireworkTrail() { return this.fireworkTrail; }

    /**
     * Checks if firework flicker is enabled
     * @return true if firework flicker effect is active
     */
    public boolean isFireworkFlicker() { return this.fireworkFlicker; }

    /**
     * Checks if coordinate display is enabled
     * @return true if death coordinates are shown
     */
    public boolean isCoordsEnabled() { return this.coordsEnabled; }

    /**
     * Checks if sound effects are enabled
     * @return true if suicide sounds are played
     */
    public boolean isSoundEnabled() { return this.soundEnabled; }

    /**
     * Gets list of disabled worlds
     * @return List of world names where command is disabled
     */
    public List<String> getDisabledWorlds() { return this.disabledWorlds; }

    /**
     * Gets broadcast message templates
     * @return List of possible broadcast messages
     */
    public List<String> getBroadcastMessages() { return this.broadcastMessages; }
}
