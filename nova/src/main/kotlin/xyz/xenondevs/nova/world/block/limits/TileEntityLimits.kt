package xyz.xenondevs.nova.world.block.limits

import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED

internal object TileEntityLimits {
    
    private val limiters: List<BlockLimiter> by MAIN_CONFIG.entry<List<BlockLimiter>>("performance", "tile_entity_limits")
    
    fun canPlace(ctx: BlockPlaceContext): PlaceResult {
        val block = ctx.item.novaItem?.block as? NovaTileEntityBlock
            ?: return ALLOWED
        
        limiters.forEach {
            val result = it.canPlace(block, ctx)
            if (!result.allowed)
                return result
        }
        
        return ALLOWED
    }
    
    data class PlaceResult(val allowed: Boolean, val message: String)
    
}
