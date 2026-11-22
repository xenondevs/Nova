package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.ui.waila.WailaToolIconProvider

/**
 * A builder for [WailaToolIconProvider].
 */
@RegistryElementBuilderDsl
sealed interface WailaToolIconProviderBuilder : RegistryEntryBuilder.Nova<WailaToolIconProvider> {
    
    /**
     * Configures the icon provider logic.
     *
     * The lambda should return the ids of the tool textures corresponding to the given block tags,
     * or an empty set if this provider cannot provide an icon for the given parameters.
     */
    fun iconProvider(iconGetter: (tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>)
    
}