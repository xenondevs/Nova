package xyz.xenondevs.nova.api.event.protection

import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

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
    
    override fun toString(): String {
        return "ProtectionCheckEvent(source=$source, type=$type, location=$location, allowed=$allowed)"
    }
    
    enum class ProtectionType {
        BREAK,
        PLACE,
        USE_BLOCK,
        USE_ITEM,
        INTERACT_ENTITY,
        HURT_ENTITY
    }
    
}