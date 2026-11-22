package xyz.xenondevs.nova.ui.waila

import kotlinx.serialization.Serializable
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderSerializer

/**
 * Serializable type alias for `RegistryEntry.Nova<WailaInfoProvider<*>>` using [WailaInfoProviderEntrySerializer].
 */
typealias WailaInfoProviderEntry = @Serializable(with = WailaInfoProviderEntrySerializer::class) RegistryEntry.Nova<WailaInfoProvider<*>>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<WailaInfoProvider<*>>` using [WailaInfoProviderEntrySetSerializer].
 */
typealias WailaInfoProviderEntrySet = @Serializable(with = WailaInfoProviderEntrySetSerializer::class) RegistryEntrySet.Nova<WailaInfoProvider<*>>

/**
 * Provides the [WailaInfo] (icon and text) for a player looking at a block.
 */
@Serializable(with = WailaInfoProviderSerializer::class)
class WailaInfoProvider<in S : BlockData> internal constructor(
    override val entry: RegistryEntry.Nova<WailaInfoProvider<S>>,
    /**
     * The blocks this provider applies to.
     */
    val blocks: Provider<Set<BlockType>>,
    /**
     * The priority of this provider.
     * If multiple providers apply to the same block, the one with the highest priority will be chosen.
     */
    val priority: Int,
    private val infoGetter: (player: Player, block: Block, blockState: S) -> WailaInfo
) : NovaRegistryElement<WailaInfoProvider<S>> {
    /**
     * Returns the info for [player] looking at [block].
     * The provider callback receives a clone of [blockState] that it may modify.
     */
    @Suppress("UNCHECKED_CAST")
    fun getInfo(player: Player, block: Block, blockState: S): WailaInfo = infoGetter(player, block, blockState.clone() as S)
}
