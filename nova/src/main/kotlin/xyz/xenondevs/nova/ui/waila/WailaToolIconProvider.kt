package xyz.xenondevs.nova.ui.waila

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.block.BlockType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderSerializer
import xyz.xenondevs.nova.ui.overlay.MovedFonts

/**
 * Serializable type alias for `RegistryEntry.Nova<WailaToolIconProvider>` using [WailaToolIconProviderEntrySerializer].
 */
typealias WailaToolIconProviderEntry = @Serializable(with = WailaToolIconProviderEntrySerializer::class) RegistryEntry.Nova<WailaToolIconProvider>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<WailaToolIconProvider>` using [WailaToolIconProviderEntrySetSerializer].
 */
typealias WailaToolIconProviderEntrySet = @Serializable(with = WailaToolIconProviderEntrySetSerializer::class) RegistryEntrySet.Nova<WailaToolIconProvider>

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
     */
    fun getIcon(tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>): Set<Key> = iconGetter(tags)
    
    companion object {
        
        internal val TOOL_ICON_IDS: Provider<Map<BlockType, Set<Key>>> =
            combinedProvider(
                NovaRegistries.WAILA_TOOL_ICON_PROVIDER.entrySet, BlockTypeTags.TAGS_BY_ELEMENT
            ) { iconProviders, tagsByBlockType ->
                tagsByBlockType.mapValues { [_, tags] -> iconProviders.flatMapTo(LinkedHashSet()) { it.getIcon(tags) } }
            }
        
        private val TOOL_ICONS: Map<BlockType, List<Component>>
            by TOOL_ICON_IDS.map { idsByBlockType ->
                combinedProvider(
                    idsByBlockType.map { [blockType, ids] ->
                        combinedProvider(ids.map(TextureIconContent::getIcon)) { icons ->
                            val components = icons.mapNotNull { icon ->
                                icon
                                    ?.component
                                    ?.shadowColor(ShadowColor.none())
                                    ?.let { component -> MovedFonts.moveVertically(component, 1) }
                            }
                            blockType to components
                        }
                    }
                ) { it.toMap() }
            }.flatten()
        
        /**
         * Returns the [Components][Component] for each tool icon of [blockType].
         */
        fun getToolIcons(blockType: BlockType): List<Component> =
            TOOL_ICONS[blockType] ?: emptyList()
        
    }
    
}