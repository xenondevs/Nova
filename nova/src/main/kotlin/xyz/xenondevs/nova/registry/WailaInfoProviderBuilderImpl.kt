package xyz.xenondevs.nova.registry

import org.bukkit.Keyed
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.world.BlockPos

internal class WailaInfoProviderBuilderImpl<B : Keyed, S : Any>(
    override val entry: RegistryEntry.Nova<WailaInfoProvider<B, S>>,
) : WailaInfoProviderBuilder<B, S>, RegistryElementBuilder.Nova<WailaInfoProvider<B, S>> {
    
    override var blocks = emptyRegistryEntrySet<B>()
    override var priority = 0
    
    private var infoGetter: (Player, BlockPos, S) -> WailaInfo = { _, _, _ -> throw NotImplementedError() }
    
    override fun infoProvider(getInfo: (player: Player, pos: BlockPos, blockState: S) -> WailaInfo) {
        infoGetter = getInfo
    }
    
    override fun infoProvider(
        base: RegistryEntry.Nova<WailaInfoProvider<B, S>>,
        modifyInfo: (player: Player, pos: BlockPos, blockState: S, info: WailaInfo) -> WailaInfo
    ) {
        infoGetter = { player, pos, state ->
            val baseInfo = base.get().getInfo(player, pos, state)
            modifyInfo(player, pos, state, baseInfo)
        }
    }
    
    override fun build() = WailaInfoProvider(entry, blocks, priority, infoGetter)
    
}