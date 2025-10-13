package xyz.xenondevs.nova.world.block.limits

import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED

internal object TileEntityLimits {
    
    private val limiters: List<BlockLimiter>? by MAIN_CONFIG.optionalEntry<List<BlockLimiter>>("performance", "tile_entity_limits")
    
    fun canPlace(ctx: Context<BlockPlace>): PlaceResult {
        val block: NovaTileEntityBlock = ctx[BlockPlace.BLOCK_TYPE_NOVA] as? NovaTileEntityBlock
            ?: return ALLOWED
        
        limiters?.forEach {
            val result = it.canPlace(block, ctx)
            if (!result.allowed)
                return result
        }
        
        return ALLOWED
    }
    
    data class PlaceResult(val allowed: Boolean, val message: String)
    
}
