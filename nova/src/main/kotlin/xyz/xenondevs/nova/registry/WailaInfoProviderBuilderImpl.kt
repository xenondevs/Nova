package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider

internal class WailaInfoProviderBuilderImpl<S : BlockData>(
    override val entry: RegistryEntry.Nova<WailaInfoProvider<S>>,
) : WailaInfoProviderBuilder<S>, RegistryElementBuilder.Nova<WailaInfoProvider<S>> {
    
    override val tags: Provider<Set<RegistryEntrySet.Nova.Tag<WailaInfoProvider<S>>>>
        field = mutableProvider(emptySet())
    
    override var blocks: RegistryEntrySet.Paper<BlockType> = emptyRegistryEntrySet(RegistryKey.BLOCK)
    override var priority = 0
    
    private var infoGetter: (Player, Block, S) -> WailaInfo = { _, _, _ -> throw NotImplementedError() }
    
    override fun tags(vararg tags: RegistryEntrySet.Nova.Tag<WailaInfoProvider<S>>) {
        this.tags.set(this.tags.get() + tags)
    }
    
    override fun infoProvider(getInfo: (player: Player, block: Block, blockState: S) -> WailaInfo) {
        infoGetter = getInfo
    }
    
    override fun infoProvider(
        base: RegistryEntry.Nova<WailaInfoProvider<S>>,
        modifyInfo: (Player, Block, S, WailaInfo) -> WailaInfo
    ) {
        infoGetter = { player, block, state ->
            val baseInfo = base.get().getInfo(player, block, state)
            modifyInfo(player, block, state, baseInfo)
        }
    }
    
    override fun build() = WailaInfoProvider(entry, blocks, priority, infoGetter)
    
}
