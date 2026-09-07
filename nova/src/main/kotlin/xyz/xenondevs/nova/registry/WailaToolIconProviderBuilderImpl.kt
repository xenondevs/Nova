package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider

internal class WailaToolIconProviderBuilderImpl(
    override val entry: RegistryEntry.Nova<WailaToolIconProvider>
) : WailaToolIconProviderBuilder, RegistryElementBuilder.Nova<WailaToolIconProvider> {
    
    override val tags: Provider<Set<RegistryEntrySet.Nova.Tag<WailaToolIconProvider>>>
        field = mutableProvider(emptySet())
    
    private var iconGetter: ((tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>) = { emptySet() }
    
    override fun tags(vararg tags: RegistryEntrySet.Nova.Tag<WailaToolIconProvider>) {
        this.tags.set(this.tags.get() + tags)
    }
    
    override fun iconProvider(iconGetter: (tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>) -> Set<Key>) {
        this.iconGetter = iconGetter
    }
    
    override fun build() = WailaToolIconProvider(entry, iconGetter)
    
}