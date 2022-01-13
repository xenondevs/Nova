package xyz.xenondevs.nova.api.event.protection

import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

class ProtectionCheckEvent(val source: Source, item: ItemStack?, val type: ProtectionType, val location: Location) : Event() {
    
    val item = item?.clone()
    
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
        return "ProtectionCheckEvent(source=$source, type=$type, location=$location, item=$item, allowed=$allowed)"
    }
    
    enum class ProtectionType {
        BREAK,
        PLACE,
        USE_BLOCK,
        USE_ITEM
    }
    
}