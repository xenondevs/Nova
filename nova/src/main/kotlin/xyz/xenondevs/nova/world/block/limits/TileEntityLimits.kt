package xyz.xenondevs.nova.world.block.limits

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.material.NovaTileEntityBlock
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED
import java.util.logging.Level

internal object TileEntityLimits {
    
    private lateinit var limiters: List<BlockLimiter>
    
    init {
        reload()
    }
    
    fun reload() {
        val limiters = ArrayList<BlockLimiter>()
        
        try {
            val blockLimits = DEFAULT_CONFIG.getConfigurationSection("performance.tile_entity_limits")
            blockLimits?.getKeys(false)?.forEach { type ->
                val cfgValue = blockLimits.get(type) ?: blockLimits.getConfigurationSection(type) ?: return@forEach
                val limiter = BlockLimiter.createNew(type, cfgValue) ?: return@forEach
                limiters += limiter
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "An exception occurred trying to load block limits (invalid configuration?)", e)
        }
        
        this.limiters = limiters
    }
    
    
    fun canPlace(ctx: BlockPlaceContext): PlaceResult {
        val material = ctx.item.novaMaterial as? NovaTileEntityBlock
            ?: return ALLOWED
        
        limiters.forEach {
            val result = it.canPlace(material, ctx)
            if (!result.allowed)
                return result
        }
        
        return ALLOWED
    }
    
    data class PlaceResult(val allowed: Boolean, val message: String)
    
}
