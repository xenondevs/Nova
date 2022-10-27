package xyz.xenondevs.nova.world.block.limits

import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits.PlaceResult

internal interface BlockLimiter {
    
    fun canPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): PlaceResult
    
    companion object {
        
        val ALLOWED = PlaceResult(true, "")
        
        @Suppress("UNCHECKED_CAST")
        fun createNew(type: String, cfg: Any): BlockLimiter? {
            return when (type) {
                "type" -> TypeBlacklist(cfg as List<String>)
                "world" -> WorldBlacklist(if (cfg is ConfigurationSection) cfg.getStringList("worlds") else cfg as List<String>)
                "type_world" -> TypeWorldBlacklist(cfg as ConfigurationSection)
                "amount" -> AmountLimiter(cfg as ConfigurationSection, AmountLimiter.Type.GLOBAL)
                "amount_per_world" -> AmountLimiter(cfg as ConfigurationSection, AmountLimiter.Type.PER_WORLD)
                "amount_per_chunk" -> AmountLimiter(cfg as ConfigurationSection, AmountLimiter.Type.PER_CHUNK)
                else -> null
            }
        }
        
    }
    
}

internal abstract class SimpleBlockLimiter(denyMessage: String) : BlockLimiter {
    
    private val denied = PlaceResult(false, denyMessage)
    
    final override fun canPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): PlaceResult {
        return if (testPlace(material, ctx)) ALLOWED else denied
    }
    
    abstract fun testPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): Boolean
    
}

internal class TypeBlacklist(blacklist: List<String>) : SimpleBlockLimiter("nova.tile_entity_limits.type_blacklist.deny") {
    
    private val blacklist = blacklist.mapTo(HashSet(), NamespacedId::of)
    
    override fun testPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): Boolean {
        return material.id !in blacklist
    }
    
}

internal class WorldBlacklist(blacklist: List<String>) : SimpleBlockLimiter("nova.tile_entity_limits.world_blacklist.deny") {
    
    private val blacklist = blacklist.toHashSet()
    
    override fun testPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): Boolean {
        return !blacklist.contains("*") && ctx.pos.world.name !in blacklist
    }
    
}

internal class TypeWorldBlacklist(cfg: ConfigurationSection) : SimpleBlockLimiter("nova.tile_entity_limits.type_world_blacklist.deny") {
    
    private val blacklist: Map<String, Set<NamespacedId>> =
        cfg.getKeys(false).associateWithTo(HashMap()) { world -> cfg.getStringList(world).mapTo(HashSet(), NamespacedId::of) }
    
    override fun testPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): Boolean {
        val id = material.id
        return blacklist["*"]?.contains(id) != true && blacklist[ctx.pos.world.name]?.contains(id) != true
    }
    
}

internal class AmountLimiter(cfg: ConfigurationSection, private val type: Type) : BlockLimiter {
    
    private val limits: Map<NamespacedId?, Int> =
        cfg.getKeys(false).associateTo(HashMap()) { id -> (if (id == "*") null else NamespacedId.of(id)) to cfg.getInt(id) }
    
    private val deniedSpecific = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}.deny")
    private val deniedTotal = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}_total.deny")
    
    override fun canPlace(material: BlockNovaMaterial, ctx: BlockPlaceContext): PlaceResult {
        val id = material.id
        
        val specificLimit = limits[id]
        val totalLimit = limits[null]
        
        if (specificLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, id)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.world.uid, id)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.chunkPos, id)
            }
            
            if (amount >= specificLimit)
                return deniedSpecific
        }
        
        if (totalLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.world.uid)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.chunkPos)
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