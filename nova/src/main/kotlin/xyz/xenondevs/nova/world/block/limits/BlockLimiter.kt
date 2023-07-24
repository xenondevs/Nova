package xyz.xenondevs.nova.world.block.limits

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits.PlaceResult

internal interface BlockLimiter {
    
    fun canPlace(material: NovaBlock, ctx: BlockPlaceContext): PlaceResult
    
    companion object {
        
        val ALLOWED = PlaceResult(true, "")
        
    }
    
}

internal abstract class SimpleBlockLimiter(denyMessage: String) : BlockLimiter {
    
    private val denied = PlaceResult(false, denyMessage)
    
    final override fun canPlace(material: NovaBlock, ctx: BlockPlaceContext): PlaceResult {
        return if (testPlace(material, ctx)) ALLOWED else denied
    }
    
    abstract fun testPlace(material: NovaBlock, ctx: BlockPlaceContext): Boolean
    
}

internal class TypeBlacklist(private val blacklist: Set<ResourceLocation>) : SimpleBlockLimiter("nova.tile_entity_limits.type_blacklist.deny") {
    
    override fun testPlace(material: NovaBlock, ctx: BlockPlaceContext): Boolean {
        return material.id !in blacklist
    }
    
}

internal class WorldBlacklist(private val blacklist: Set<String>) : SimpleBlockLimiter("nova.tile_entity_limits.world_blacklist.deny") {
    
    override fun testPlace(material: NovaBlock, ctx: BlockPlaceContext): Boolean {
        return !blacklist.contains("*") && ctx.pos.world.name !in blacklist
    }
    
}

internal class TypeWorldBlacklist(private val blacklist: Map<String, Set<ResourceLocation>>) : SimpleBlockLimiter("nova.tile_entity_limits.type_world_blacklist.deny") {
    
    override fun testPlace(material: NovaBlock, ctx: BlockPlaceContext): Boolean {
        val id = material.id
        return blacklist["*"]?.contains(id) != true && blacklist[ctx.pos.world.name]?.contains(id) != true
    }
    
}

internal class AmountLimiter(private val type: Type, private val limits: Map<ResourceLocation?, Int>) : BlockLimiter {
    
    private val deniedSpecific = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}.deny")
    private val deniedTotal = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}_total.deny")
    
    override fun canPlace(material: NovaBlock, ctx: BlockPlaceContext): PlaceResult {
        val id = material.id
        val owner = ctx.ownerUUID ?: return ALLOWED
        
        val specificLimit = limits[id]
        val totalLimit = limits[null]
        
        if (specificLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(owner, id)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(owner, ctx.pos.world.uid, id)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(owner, ctx.pos.chunkPos, id)
            }
            
            if (amount >= specificLimit)
                return deniedSpecific
        }
        
        if (totalLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(owner)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(owner, ctx.pos.world.uid)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(owner, ctx.pos.chunkPos)
            }
            
            if (amount >= totalLimit)
                return deniedTotal
        }
        
        return ALLOWED
    }
    
    enum class Type {
        GLOBAL,
        PER_WORLD,
        PER_CHUNK
    }
    
}