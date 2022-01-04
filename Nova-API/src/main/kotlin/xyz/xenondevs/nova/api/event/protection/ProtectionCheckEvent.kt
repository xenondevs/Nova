package xyz.xenondevs.nova.api.event.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.api.TileEntity

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
    
    open class Source
    
    class PlayerSource(val player: OfflinePlayer) : Source()
    
    class TileEntitySource(val tileEntity: TileEntity) : Source()
    
}