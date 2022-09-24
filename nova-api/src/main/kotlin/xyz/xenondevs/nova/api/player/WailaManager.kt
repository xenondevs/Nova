package xyz.xenondevs.nova.api.player

import org.bukkit.entity.Player

interface WailaManager {
    
    /**
     * If WAILA is completely disabled using the main config.
     */
    val isCompletelyDisabled: Boolean
    
    /**
     * Gets the current on/off state of WAILA for specific player.
     */
    fun getState(player: Player): Boolean
    
    /**
     * Sets the on/off state of WAILA for [player] to [enabled].
     *
     * @return The previous state (if WAILA was enabled previously)
     */
    fun setState(player: Player, enabled: Boolean): Boolean
    
}