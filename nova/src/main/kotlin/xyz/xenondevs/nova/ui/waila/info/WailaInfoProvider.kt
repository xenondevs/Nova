package xyz.xenondevs.nova.ui.waila.info

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderSerializer

data class WailaLine(val text: Component, val alignment: Alignment) {
    
    enum class Alignment {
        LEFT,
        CENTERED,
        FIRST_LINE,
        PREVIOUS_LINE
    }
    
}

data class WailaInfo(val icon: Key, val lines: List<WailaLine>)

/**
 * Provides the [WailaInfo] (icon and text) for a player looking at a block.
 */
@Serializable(with = WailaInfoProviderSerializer::class)
class WailaInfoProvider<in S : BlockData> internal constructor(
    override val entry: RegistryEntry.Nova<WailaInfoProvider<S>>,
    /**
     * The blocks this provider applies to.
     */
    val blocks: RegistryEntrySet.Paper<BlockType>,
    /**
     * The priority of this provider.
     * If multiple providers apply to the same block, the one with the highest priority will be chosen.
     */
    val priority: Int,
    private val infoGetter: (player: Player, block: Block, blockState: S) -> WailaInfo
) : NovaRegistryElement<WailaInfoProvider<S>> {
    fun getInfo(player: Player, block: Block, blockState: S): WailaInfo = infoGetter(player, block, blockState)
}
