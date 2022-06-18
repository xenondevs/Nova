package xyz.xenondevs.nova.player

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.runTaskTimer

/**
 * A class wrapping Bukkit's [PlayerInteractEvent], which will not be called if the
 * event for the other hand has been cancelled.
 */
class WrappedPlayerInteractEvent(val event: PlayerInteractEvent) : Event()  {
    
    companion object : Listener {
        
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
        
        private val cancelledInteracts = HashSet<Pair<Player, Action>>()
        
        init {
            Bukkit.getPluginManager().registerEvents(this, NOVA)
            runTaskTimer(0, 1) { cancelledInteracts.clear() }
        }
        
        @EventHandler(priority = EventPriority.LOWEST)
        fun handleInteract(event: PlayerInteractEvent) {
            if (event.isCompletelyDenied()) return
            
            val pair = event.player to event.action
            if (pair in cancelledInteracts) {
                event.isCancelled = true
            } else {
                Bukkit.getPluginManager().callEvent(WrappedPlayerInteractEvent(event))
                if (event.isCompletelyDenied())
                    cancelledInteracts += event.player to event.action
            }
        }
        
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}