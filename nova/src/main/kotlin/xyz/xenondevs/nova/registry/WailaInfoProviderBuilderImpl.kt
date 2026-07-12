package xyz.xenondevs.nova.registry

import org.bukkit.Keyed
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider

internal class WailaInfoProviderBuilderImpl<B : Keyed, S : Any>(
    override val entry: RegistryEntry.Nova<WailaInfoProvider<B, S>>,
) : WailaInfoProviderBuilder<B, S>, RegistryElementBuilder.Nova<WailaInfoProvider<B, S>> {
    
    override var blocks = emptyRegistryEntrySet<B>()
    override var priority = 0
    
    private var infoGetter: (Player, Block, S) -> WailaInfo = { _, _, _ -> throw NotImplementedError() }
    
    override fun infoProvider(getInfo: (player: Player, block: Block, blockState: S) -> WailaInfo) {
        infoGetter = getInfo
    }
    
    override fun infoProvider(
        base: RegistryEntry.Nova<WailaInfoProvider<B, S>>,
        modifyInfo: (Player, Block, S, WailaInfo) -> WailaInfo
    ) {
        infoGetter = { player, block, state ->
            val baseInfo = base.get().getInfo(player, block, state)
            modifyInfo(player, block, state, baseInfo)
        }
    }
    
    override fun build() = WailaInfoProvider(entry, blocks, priority, infoGetter)
    
}
