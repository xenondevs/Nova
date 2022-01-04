package xyz.xenondevs.nova.api.event.protection

import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ProtectionCheckEvent(val source: Source, val type: ProtectionType, val location: Location) : Event() {
    
    var allowed = true
    
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

    enum class ProtectionType {
        BREAK,
        PLACE,
        USE
    }
    
}