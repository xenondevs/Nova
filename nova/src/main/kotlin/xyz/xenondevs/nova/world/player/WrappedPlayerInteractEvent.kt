package xyz.xenondevs.nova.world.player

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer

/**
 * A class wrapping Bukkit's [PlayerInteractEvent], which will not be called for the
 * other hand if an action has been performed (marked via [WrappedPlayerInteractEvent.actionPerformed]).
 */
class WrappedPlayerInteractEvent(val event: PlayerInteractEvent) : Event() {
    
    /**
     * Whether an action has been performed (by Nova or addons).
     *
     * Possible actions might be: Nova block placed, Gui opened, custom armor equipped, etc.
     * Note that this does not include possible vanilla actions that might happen if the [PlayerInteractEvent] is not cancelled.
     *
     * If this is set to true, possible subsequent offhand events will not be fired.
     */
    var actionPerformed = false
    
    companion object : Listener {
        
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
        
        private val performedCustomInteractions = HashSet<Pair<Player, Action>>()
        
        init {
            registerEvents()
            runTaskTimer(0, 1) { performedCustomInteractions.clear() }
        }
        
        @EventHandler(priority = EventPriority.LOWEST)
        fun handleInteract(event: PlayerInteractEvent) {
            val playerAction = event.player to event.action
            if (playerAction in performedCustomInteractions) {
                event.isCancelled = true
            } else {
                val wrappedEvent = WrappedPlayerInteractEvent(event)
                Bukkit.getPluginManager().callEvent(wrappedEvent)
                if (wrappedEvent.actionPerformed)
                    performedCustomInteractions += playerAction
            }
        }
        
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}