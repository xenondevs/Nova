package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider

internal class WailaToolIconProviderBuilderImpl(
    override val entry: RegistryEntry.Nova<WailaToolIconProvider>
) : WailaToolIconProviderBuilder, RegistryElementBuilder.Nova<WailaToolIconProvider> {
    
    private var iconGetter: ((tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>) = { emptySet() }
    
    override fun iconProvider(iconGetter: (tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>) {
        this.iconGetter = iconGetter
    }
    
    override fun build() = WailaToolIconProvider(entry, iconGetter)
    
}