package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider

/**
 * A builder for [WailaToolIconProvider].
 */
@RegistryElementBuilderDsl
sealed interface WailaToolIconProviderBuilder : RegistryEntryBuilder.Nova<WailaToolIconProvider> {
    
    /**
     * Configures the icon provider logic.
     * 
     * The lambda should return the tool textures corresponding to the given block tags as a [Key],
     * or `null` if this provider cannot provide an icon for the given parameters.
     * Note that textures which are intended to be used for these icons need to be added to the texture icon font via [TextureIconContent.addIcons]
     * using a custom resource pack task.
     */
    fun iconProvider(iconGetter: (tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>)
    
}