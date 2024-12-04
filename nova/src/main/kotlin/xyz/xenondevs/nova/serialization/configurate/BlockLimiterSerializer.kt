package xyz.xenondevs.nova.serialization.configurate

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.world.block.limits.AmountLimiter
import xyz.xenondevs.nova.world.block.limits.BlockLimiter
import xyz.xenondevs.nova.world.block.limits.TypeBlacklist
import xyz.xenondevs.nova.world.block.limits.TypeWorldBlacklist
import xyz.xenondevs.nova.world.block.limits.WorldBlacklist
import java.lang.reflect.Type

internal object BlockLimiterSerializer : TypeSerializer<List<BlockLimiter>> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): List<BlockLimiter> {
        val limiters = ArrayList<BlockLimiter>()
        for ((key, value) in node.childrenMap()) {
            limiters += deserializeLimiter(key as String, value)
        }
        
        return limiters
    }
    
    private fun deserializeLimiter(type: String, node: ConfigurationNode): BlockLimiter {
        return when (type) {
            "type" -> TypeBlacklist(node.get<HashSet<Key>>()!!)
            "world" -> WorldBlacklist(node.get<HashSet<String>>()!!)
            "type_world" -> TypeWorldBlacklist(node.get<HashMap<String, HashSet<Key>>>()!!)
            "amount" -> AmountLimiter(AmountLimiter.Type.GLOBAL, deserializeAmountLimiterMap(node))
            "amount_per_world" -> AmountLimiter(AmountLimiter.Type.PER_WORLD, deserializeAmountLimiterMap(node))
            "amount_per_chunk" -> AmountLimiter(AmountLimiter.Type.PER_CHUNK, deserializeAmountLimiterMap(node))
            else -> throw IllegalArgumentException("Unknown block limiter type: $type")
        }
    }
    
    private fun deserializeAmountLimiterMap(node: ConfigurationNode): Map<Key?, Int> {
        val map = HashMap<Key?, Int>()
        for ((key, value) in node.childrenMap()) {
            key as String
            val id = if (key == "*") null else Key.key(key)
            map[id] = value.int
        }
        return map
    }
    
    override fun serialize(type: Type?, obj: List<BlockLimiter>?, node: ConfigurationNode?) {
        throw UnsupportedOperationException()
    }
    
}