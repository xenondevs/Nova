package xyz.xenondevs.nova.world.block.limits

import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED

internal object TileEntityLimits {
    
    private val limiters: List<BlockLimiter>? by MAIN_CONFIG.optionalEntry<List<BlockLimiter>>("performance", "tile_entity_limits")
    
    fun canPlace(ctx: Context<BlockPlace>): PlaceResult {
        val block: NovaTileEntityBlock = ctx[DefaultContextParamTypes.BLOCK_TYPE_NOVA] as? NovaTileEntityBlock 
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
