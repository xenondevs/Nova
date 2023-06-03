package xyz.xenondevs.nova.api.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface WailaManager {
    
    /**
     * Checks whether WAILA is completely disabled in the main config.
     *
     * @return Whether WAILA is completely disabled.
     */
    boolean isCompletelyDisabled();
    
    /**
     * Gets the current on/off state of WAILA for the specified player.
     *
     * @param player The player to check.
     * @return Whether WAILA is enabled for the specified player.
     */
    boolean getState(@NotNull Player player);
    
    /**
     * Sets the on/off state of WAILA for the specified player.
     *
     * @param player The player to set the state for.
     * @param state  The new state.
     * @return The previous state (if WAILA was enabled previously).
     */
    boolean setState(@NotNull Player player, boolean state);
    
}
