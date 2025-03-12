package me.prism3.suicide;

import com.jeff_media.updatechecker.UpdateChecker;
import me.prism3.suicide.utils.Data;
import me.prism3.suicide.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Main plugin class handling lifecycle management and core functionality.
 * Manages plugin initialization, configuration, and core components.
 *
 * @author Prism3
 * @since 1.0
 */
public class Suicide extends JavaPlugin {

    /**
     * Thread-safe collection tracking players who recently used the suicide command
     */
    private Set<UUID> players;

    /**
     * Central configuration manager instance
     */
    private Data data;

    /**
     * Handles plugin initialization and startup procedures
     */
    @Override
    public void onEnable() {
        this.initializeCoreComponents();
        this.setupMetrics();
        this.pluginUpdateChecker();

        this.getLogger().info("Plugin Enabled!");
    }

    /**
     * Handles plugin shutdown and cleanup operations
     */
    @Override
    public void onDisable() { this.getLogger().info("Plugin Disabled!"); }

    // endregion

    // region Initialization

    /**
     * Initializes core plugin components
     */
    private void initializeCoreComponents() {
        this.initializePlayerTracking();
        this.setupConfiguration();
    }

    /**
     * Creates thread-safe collection for player tracking
     */
    private void initializePlayerTracking() { this.players = ConcurrentHashMap.newKeySet(); }

    /**
     * Loads configuration files and data manager
     */
    private void setupConfiguration() {
        this.saveDefaultConfig();
        this.data = new Data(this);
    }

    /**
     * Sets up bStats metrics integration
     */
    private void setupMetrics() { new Metrics(this, 11664); }

    /**
     * Configures automatic update checking
     */
    private void pluginUpdateChecker() {
        UpdateChecker.init(this, this.data.getResourceID())
                .checkEveryXHours(4)
                .setChangelogLink(this.data.getResourceID())
                .setNotifyOpsOnJoin(true)
                .checkNow();
    }

    /**
     * Provides access to the plugin singleton instance
     *
     * @return The active plugin instance
     */
    public static Suicide getInstance() { return JavaPlugin.getPlugin(Suicide.class); }

    /**
     * Retrieves the collection of tracked players
     *
     * @return Set of player UUIDs being tracked
     */
    public Set<UUID> getPlayers() { return this.players; }

    /**
     * Provides access to configuration data
     *
     * @return Initialized Data manager instance
     */
    public Data getData() { return this.data; }
}
