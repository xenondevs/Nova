package xyz.xenondevs.nova.util

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.commons.collections.weakHashSet
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import java.util.*

/**
 * Utility for creating maps with player keys that are automatically cleaned up
 * when the player leaves the server.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object PlayerMapManager : Listener {
    
    private val activeMaps = weakHashSet<WeakHashMap<Player, *>>()
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    /**
     * Creates a new [MutableMap] with [Player] keys and a specified value type [V].
     * The map will automatically remove entries for players that leave the server.
     * The map is not synchronized, so it should only be accessed from the server thread.
     */
    fun <V> create(): MutableMap<Player, V> {
        val map = WeakHashMap<Player, V>()
        activeMaps += map
        return map
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        for (map in activeMaps) {
            map.remove(event.player)
        }
    }
    
}
