package xyz.xenondevs.nova.world.block.limits

import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

internal interface BlockLimiter {
    
    val denyMessage: String
    
    fun canPlace(ctx: BlockPlaceContext): Boolean
    
    companion object {
        
        private val limiterTypes: Map<String, (ConfigurationSection) -> BlockLimiter> = hashMapOf(
            "world" to ::WorldBlacklist,
            "type_world" to ::TypeWorldBlacklist,
            "amount" to { AmountLimiter(it, false) },
            "amount_per_world" to { AmountLimiter(it, true) }
        )
        
        fun createNew(type: String, cfg: ConfigurationSection): BlockLimiter? =
            limiterTypes[type]?.invoke(cfg)
        
    }
    
}

internal class WorldBlacklist(cfg: ConfigurationSection) : BlockLimiter {
    
    private val blacklist = cfg.getStringList("worlds").toHashSet()
    override val denyMessage = "nova.tile_entity_limits.world_blacklist.deny"
    
    override fun canPlace(ctx: BlockPlaceContext): Boolean {
        return ctx.pos.world.name !in blacklist
    }
    
}

internal class TypeWorldBlacklist(cfg: ConfigurationSection) : BlockLimiter {
    
    private val blacklist: Map<String, Set<NamespacedId>> =
        cfg.getKeys(false).associateWithTo(HashMap()) { world -> cfg.getStringList(world).mapTo(HashSet(), NamespacedId::of) }
    
    override val denyMessage = "nova.tile_entity_limits.world_type_blacklist.deny"
    
    override fun canPlace(ctx: BlockPlaceContext): Boolean {
        val id = ctx.item.novaMaterial?.id ?: return true
        return blacklist[ctx.pos.world.name]?.contains(id) != true
    }
    
}

internal class AmountLimiter(cfg: ConfigurationSection, private val perWorld: Boolean) : BlockLimiter {
    
    private val limits: Map<NamespacedId, Int> =
        cfg.getKeys(false).associateTo(HashMap()) { id -> NamespacedId.of(id) to cfg.getInt(id) }
    
    override val denyMessage = "nova.tile_entity_limits.amount" + (if (perWorld) "_per_world" else "") + ".deny"
    
    override fun canPlace(ctx: BlockPlaceContext): Boolean {
        val id = ctx.item.novaMaterial?.id ?: return true
        val limit = limits[id] ?: return true
        
        val amount = if (perWorld)
            TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.world.uid, id)
        else TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, id)
        
        return limit > amount
    }
    
}