package xyz.xenondevs.nova.api.event.tileentity

import org.bukkit.event.Event
import xyz.xenondevs.nova.api.tileentity.TileEntity

/**
 * A [TileEntity] related event.
 */
abstract class TileEntityEvent(val tileEntity: TileEntity) : Event() {
    
    override fun toString(): String {
        return "TileEntityEvent(tileEntity=$tileEntity)"
    }
    
}