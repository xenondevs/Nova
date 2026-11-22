package xyz.xenondevs.nova.ui.waila

import kotlinx.serialization.Serializable
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.serialization.kotlinx.WailaIconProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaIconProviderEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaIconProviderSerializer

/**
 * Serializable type alias for `RegistryEntry.Nova<WailaIconProvider<*>>` using [WailaIconProviderEntrySerializer].
 */
typealias WailaIconProviderEntry = @Serializable(with = WailaIconProviderEntrySerializer::class) RegistryEntry.Nova<WailaIconProvider<*>>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<WailaIconProvider<*>>` using [WailaIconProviderEntrySetSerializer].
 */
typealias WailaIconProviderEntrySet = @Serializable(with = WailaIconProviderEntrySetSerializer::class) RegistryEntrySet.Nova<WailaIconProvider<*>>

/**
 * Provides the icon for the WAILA icon of a block state.
 */
@Serializable(with = WailaIconProviderSerializer::class)
class WailaIconProvider<in S : BlockData> internal constructor(
    override val entry: RegistryEntry.Nova<WailaIconProvider<S>>,
    /**
     * The blocks this provider applies to.
     */
    val blocks: Provider<Set<BlockType>>,
    /**
     * The priority of this provider.
     * If multiple providers apply to the same block, the one with the highest priority will be chosen.
     */
    val priority: Int,
    private val iconGetter: (blockState: S) -> WailaIcon?
) : NovaRegistryElement<WailaIconProvider<S>> {
    
    /**
     * Returns the icon for [blockState], or `null` if no icon is available.
     * The provider callback receives a clone of [blockState] that it may modify.
     */
    @Suppress("UNCHECKED_CAST")
    fun getIcon(blockState: S): WailaIcon? = iconGetter(blockState.clone() as S)
    
    companion object {
        
        /**
         * Returns the font char for the rendered WAILA icon of [blockState], or `null` if no icon is available.
         */
        fun getRenderedIcon(blockState: BlockData): FontChar? =
            ResourceLookups.wailaIcon[blockState]
        
    }
    
}