package me.prism3.suicide.events;

import me.prism3.suicide.Suicide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;


/**
 * Handles player death events triggered by the suicide command.
 * Manages death message suppression and player tracking cleanup.
 *
 * @author Prism3
 * @since 1.0
 */
public class PlayerDeath implements Listener {

    /**
     * Reference to the main plugin instance
     */
    private final Suicide plugin;

    /**
     * Initializes a new PlayerDeath event listener
     */
    public PlayerDeath() {
        this.plugin = Suicide.getInstance();
    }

    /**
     * Handles player death events to suppress death messages when required
     *
     * @param event The PlayerDeathEvent being processed
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        if (shouldSilenceDeathMessage(player)) {
            handleSilentDeath(event, player);
        }
    }

    /**
     * Determines if a death message should be silenced
     *
     * @param player The player who died
     * @return true if death message should be suppressed, false otherwise
     */
    private boolean shouldSilenceDeathMessage(final Player player) {
        return plugin.getPlayers().contains(player.getUniqueId())
                && !plugin.getData().isBroadcastEnabled();
    }

    /**
     * Processes silent death cleanup operations
     *
     * @param event The death event to modify
     * @param player The player associated with the death
     */
    private void handleSilentDeath(final PlayerDeathEvent event, final Player player) {
        plugin.getPlayers().remove(player.getUniqueId());
        event.setDeathMessage(null);
    }
}
