package xyz.xenondevs.nova.world.block.limits

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer

@Serializable
internal data class BlockLimiters(
    val type: Set<@Serializable(with = KeySerializer::class) Key>? = null,
    val world: Set<String>? = null,
    @SerialName("type_world")
    val typeWorld: Map<String, Set<@Serializable(with = KeySerializer::class) Key>>? = null,
    val amount: Map<String, Int>? = null,
    @SerialName("amount_per_world")
    val amountPerWorld: Map<String, Int>? = null,
    @SerialName("amount_per_chunk")
    val amountPerChunk: Map<String, Int>? = null
) {

    fun toList(): List<BlockLimiter> = buildList {
        if (type != null) add(TypeBlacklist(type))
        if (world != null) add(WorldBlacklist(world))
        if (typeWorld != null) add(TypeWorldBlacklist(typeWorld))
        if (amount != null) add(AmountLimiter(AmountLimiter.Type.GLOBAL, parseAmountMap(amount)))
        if (amountPerWorld != null) add(AmountLimiter(AmountLimiter.Type.PER_WORLD, parseAmountMap(amountPerWorld)))
        if (amountPerChunk != null) add(AmountLimiter(AmountLimiter.Type.PER_CHUNK, parseAmountMap(amountPerChunk)))
    }

    private fun parseAmountMap(map: Map<String, Int>): Map<Key?, Int> =
        map.entries.associate { (key, value) ->
            (if (key == "*") null else Key.key(key)) to value
        }

}
