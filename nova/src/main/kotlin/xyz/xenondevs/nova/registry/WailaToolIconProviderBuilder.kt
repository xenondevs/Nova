package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier

/**
 * A builder for [WailaToolIconProvider].
 */
@RegistryElementBuilderDsl
sealed interface WailaToolIconProviderBuilder : RegistryEntryBuilder.Nova<WailaToolIconProvider> {
    
    /**
     * Configures the icon provider logic.
     * 
     * The lambda should return a [Key] for the location of the texture to be used for tools of the given category and tier,
     * or `null` if this provider cannot provide an icon for the given parameters.
     * Note that textures which are intended to be used for these icons need to be added to the texture icon font via [TextureIconContent.addIcons]
     * using a custom resource pack task.
     */
    fun iconProvider(iconGetter: (category: RegistryEntry.Nova<ToolCategory>, tier: RegistryEntry.Nova<ToolTier>?) -> Key?)
    
}