package xyz.xenondevs.nova.ui.waila.info

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderSerializer

/**
 * Serializable type alias for `RegistryEntry.Nova<WailaToolIconProvider>` using [WailaToolIconProviderEntrySerializer].
 */
typealias NovaWailaToolIconProviderEntry = @Serializable(with = WailaToolIconProviderEntrySerializer::class) RegistryEntry.Nova<WailaToolIconProvider>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<WailaToolIconProvider>` using [WailaToolIconProviderEntrySetSerializer].
 */
typealias NovaWailaToolIconProviderEntrySet = @Serializable(with = WailaToolIconProviderEntrySetSerializer::class) RegistryEntrySet.Nova<WailaToolIconProvider>

/**
 * Chooses tool icons for WAILA.
 */
@Serializable(with = WailaToolIconProviderSerializer::class)
class WailaToolIconProvider internal constructor(
    override val entry: RegistryEntry.Nova<WailaToolIconProvider>,
    private val iconGetter: (Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>
) : NovaRegistryElement<WailaToolIconProvider> {
    
    /**
     * Returns a set of [Keys][Key] for tool texture icons corresponding to [tags].
     * Note that textures which are intended to be used for these icons need to be added to the texture icon font via [TextureIconContent.addIcons]
     * using a custom resource pack task.
     */
    fun getIcon(tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>): Set<Key> = iconGetter(tags)
    
}