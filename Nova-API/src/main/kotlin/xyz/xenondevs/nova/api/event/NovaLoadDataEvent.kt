package xyz.xenondevs.nova.api.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NovaLoadDataEvent : Event(true) {
    
    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()
        
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
        
    }
    
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
    
}