package xyz.xenondevs.nova.ui.waila.info

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.ui.waila.info.impl.BellWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CakeWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CampfireWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CandleWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CauldronWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CocoaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.ComparatorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CropWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DaylightDetectorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultNovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultVanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.LanternWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RailWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RepeaterWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RespawnAnchorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.SeaPickleWailaInfoProvider
import xyz.xenondevs.nova.world.BlockPos

object WailaInfoProviderRegistry {
    
    private val providers = ArrayList<WailaInfoProvider<*>>()
    
    init {
        registerProvider(DefaultVanillaWailaInfoProvider)
        registerProvider(DefaultNovaWailaInfoProvider)
        
        registerProvider(CandleWailaInfoProvider)
        registerProvider(CakeWailaInfoProvider)
        registerProvider(CauldronWailaInfoProvider)
        registerProvider(SeaPickleWailaInfoProvider)
        registerProvider(CampfireWailaInfoProvider)
        registerProvider(CropWailaInfoProvider)
        registerProvider(RepeaterWailaInfoProvider)
        registerProvider(ComparatorWailaInfoProvider)
        registerProvider(RailWailaInfoProvider)
        registerProvider(RespawnAnchorWailaInfoProvider)
        registerProvider(LanternWailaInfoProvider)
        registerProvider(DaylightDetectorWailaInfoProvider)
        registerProvider(BellWailaInfoProvider)
        registerProvider(CocoaWailaInfoProvider)
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
                .lastOrNull { it.materials == null || material in it.materials }
                ?.getInfo(player, novaState)
        } else {
            val block = pos.block
            val type = block.type
         
            return providers.asSequence()
                .filterIsInstance<VanillaWailaInfoProvider>()
                .lastOrNull { it.materials == null || type in it.materials }
                ?.getInfo(player, block)
        }
    }
    
}