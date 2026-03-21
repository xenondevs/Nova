package xyz.xenondevs.nova.registry

import org.bukkit.Keyed
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.world.BlockPos

/**
 * A builder for [WailaInfoProvider].
 */
@RegistryElementBuilderDsl
sealed interface WailaInfoProviderBuilder<B : Keyed, S : Any> {
    
    /**
     * The priority of this waila info provider.
     * 
     * If multiple providers that can provide info for the same block exist,
     * the one with the highest priority will be chosen.
     */
    var priority: Int
    
    /**
     * The blocks that this waila info provider applies to.
     */
    var blocks: RegistryEntrySet<B>
    
    /**
     * Sets the actual functionality of the info provider.
     * The supplied lambda will be invoked when the given player looks at the given block state at the given position.
     */
    fun infoProvider(getInfo: (player: Player, pos: BlockPos, blockState: S) -> WailaInfo)
    
    /**
     * Sets the actual functionality of the info provider.
     * First calls [base], then further modifies the info in [modifyInfo].
     * The supplied lambda will be invoked when the given player looks at the given block state at the given position.
     */
    fun infoProvider(
        base: RegistryEntry.Nova<WailaInfoProvider<B, S>>,
        modifyInfo: (player: Player, pos: BlockPos, blockState: S, info: WailaInfo) -> WailaInfo
    )
    
}