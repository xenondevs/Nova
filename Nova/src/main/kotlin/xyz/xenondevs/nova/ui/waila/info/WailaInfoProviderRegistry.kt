package xyz.xenondevs.nova.ui.waila.info

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultNovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultVanillaWailaInfoProvider
import xyz.xenondevs.nova.world.BlockPos

object WailaInfoProviderRegistry {
    
    private val providers = ArrayList<WailaInfoProvider<*>>()
    
    init {
        registerProvider(DefaultVanillaWailaInfoProvider)
        registerProvider(DefaultNovaWailaInfoProvider)
    }
    
    fun registerProvider(provider: WailaInfoProvider<*>) {
        providers += provider
    }
    
    fun getInfo(player: Player, pos: BlockPos): WailaInfo? {
        var novaState = WorldDataManager.getBlockState(pos)
        if (novaState is LinkedBlockState)
            novaState = novaState.blockState
        
        if (novaState is NovaBlockState) {
            val material = novaState.material
            
            return providers.asSequence()
                .filterIsInstance<NovaWailaInfoProvider>()
                .firstOrNull { it.materials == null || material in it.materials }
                ?.getInfo(player, novaState)
        } else {
            val block = pos.block
            val type = block.type
         
            return providers.asSequence()
                .filterIsInstance<VanillaWailaInfoProvider>()
                .firstOrNull { it.materials == null || type in it.materials }
                ?.getInfo(player, block)
        }
    }
    
}