package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.ui.waila.WailaIconProvider
import xyz.xenondevs.nova.ui.waila.WailaIcon

internal class WailaIconProviderBuilderImpl<S : BlockData>(
    override val entry: RegistryEntry.Nova<WailaIconProvider<S>>,
) : WailaIconProviderBuilder<S>, RegistryElementBuilder.Nova<WailaIconProvider<S>> {
    
    override val tags = mutableProvider(emptySet<RegistryEntrySet.Nova.Tag<WailaIconProvider<S>>>())
    override var blocks: Provider<Set<BlockType>> = emptyRegistryEntrySet(RegistryKey.BLOCK)
    override var priority = 0
    
    private var iconGetter: (S) -> WailaIcon? = { _ -> throw NotImplementedError() }
    
    override fun tags(vararg tags: RegistryEntrySet.Nova.Tag<WailaIconProvider<S>>) {
        this.tags.set(this.tags.get() + tags)
    }
    
    override fun iconProvider(getIcon: (blockState: S) -> WailaIcon?) {
        iconGetter = getIcon
    }
    
    override fun build() = WailaIconProvider(entry, blocks, priority, iconGetter)
    
}
