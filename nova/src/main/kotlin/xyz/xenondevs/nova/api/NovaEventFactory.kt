@file:Suppress("unused")

package xyz.xenondevs.nova.api

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.event.tileentity.TileEntityBreakBlockEvent
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.util.callEvent

/**
 * Intended to be used by addons to create and call events for Nova's plugin api.
 */
object NovaEventFactory {
    
    /**
     * Calls the [TileEntityBreakBlockEvent] for the given [tileEntity] and [block].
     *
     * The event might mutate the [drops] list, which should affect the dropped items.
     */
    fun callTileEntityBlockBreakEvent(tileEntity: TileEntity, block: Block, drops: MutableList<ItemStack>) {
        TileEntityBreakBlockEvent(ApiTileEntityWrapper(tileEntity), block, drops).also(::callEvent)
    }
    
}