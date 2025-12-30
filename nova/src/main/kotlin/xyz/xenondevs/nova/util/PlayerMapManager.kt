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
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for creating maps with player keys that are automatically cleaned up
 * when the player leaves the server.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object PlayerMapManager : Listener {
    
    private val activeMaps = Collections.synchronizedSet(weakHashSet<MutableMap<Player, *>>())
    private val activeSets = Collections.synchronizedSet(weakHashSet<MutableSet<Player>>())
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    /**
     * Creates a new [MutableMap] with [Player] keys and a specified value type [V].
     * The map will automatically remove entries for players that leave the server.
     * The map is not synchronized, so it should only be accessed from the server thread.
     */
    fun <V> createMap(): MutableMap<Player, V> {
        val map = HashMap<Player, V>()
        activeMaps += map
        return map
    }
    
    /**
     * Creates a new concurrent [MutableMap] with [Player] keys and a specified value type [V].
     * The map will automatically remove entries for players that leave the server.
     */
    fun <V : Any> createConcurrent(): MutableMap<Player, V> {
        val map = ConcurrentHashMap<Player, V>()
        activeMaps += map
        return map
    }
    
    /**
     * Creates a new [MutableSet] with [Player] elements.
     * The set will automatically remove players that leave the server.
     * The set is not synchronized, so it should only be accessed from the server thread.
     */
    fun createSet(): MutableSet<Player> {
        val set = mutableSetOf<Player>()
        activeSets += set
        return set
    }
    
    /**
     * Creates a new concurrent [MutableSet] with [Player] elements.
     * The set will automatically remove players that leave the server.
     */
    fun createConcurrentSet(): MutableSet<Player> {
        val set = ConcurrentHashMap.newKeySet<Player>()
        activeSets += set
        return set
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        synchronized(activeMaps) {
            for (map in activeMaps) {
                map.remove(event.player)
            }
        }
        synchronized(activeSets) {
            for (set in activeSets) {
                set.remove(event.player)
            }
        }
    }
    
}
