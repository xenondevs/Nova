package xyz.xenondevs.nova.registry

import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.ui.waila.WailaIcon
import xyz.xenondevs.nova.ui.waila.WailaIconProvider

/**
 * A builder for [WailaIconProvider].
 */
@RegistryElementBuilderDsl
sealed interface WailaIconProviderBuilder<S : BlockData> : RegistryEntryBuilder.Nova<WailaIconProvider<S>> {
    
    /**
     * The priority of this waila icon provider.
     *
     * If multiple providers that can provide icons for the same block exist,
     * the one with the highest priority will be chosen.
     */
    var priority: Int
    
    /**
     * The blocks that this waila icon provider applies to.
     */
    var blocks: Provider<Set<BlockType>>
    
    /**
     * Sets the actual functionality of the icon provider.
     * The supplied lambda returns the icon for the given block state, or `null` if no icon is available.
     */
    fun iconProvider(getIcon: (blockState: S) -> WailaIcon?)
    
}
