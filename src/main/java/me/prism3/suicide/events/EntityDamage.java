package me.prism3.suicide.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;


/**
 * Prevents damage from fireworks spawned by the suicide command.
 * Identifies plugin-generated fireworks through metadata tags.
 *
 * @author Prism3
 * @since 1.0
 */
public class EntityDamage implements Listener {

    /**
     * Metadata key used to identify safe fireworks
     */
    private static final String NO_DAMAGE_META = "noDamage";

    /**
     * Handles entity damage events caused by fireworks
     *
     * @param event The EntityDamageByEntityEvent being processed
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(final EntityDamageByEntityEvent event) {
        if (this.isProtectedFirework(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the damager is a protected firework
     *
     * @param damager The entity causing damage
     * @return true if damage should be cancelled, false otherwise
     */
    private boolean isProtectedFirework(final Entity damager) {
        if (damager instanceof Firework firework) {
            return firework.hasMetadata(NO_DAMAGE_META);
        }
        return false;
    }
}
