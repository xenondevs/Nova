package xyz.xenondevs.nova.ui.waila.info

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
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
import xyz.xenondevs.nova.ui.waila.info.impl.RedstoneLampWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RepeaterWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RespawnAnchorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.SeaPickleWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager

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
        registerProvider(CocoaWailaInfoProvider)
        registerProvider(RedstoneLampWailaInfoProvider)
    }
    
    fun registerProvider(provider: WailaInfoProvider<*>) {
        providers += provider
    }
    
    fun getInfo(player: Player, pos: BlockPos): WailaInfo? {
        val novaState = BlockManager.getBlock(pos)
        if (novaState is NovaBlockState) {
            val material = novaState.material
            
            return providers.asSequence()
                .filterIsInstance<NovaWailaInfoProvider>()
                .lastOrNull { it.materials == null || material in it.materials }
                ?.getInfo(player, novaState)
        } else {
            val block = pos.block
            val type = block.type
            
            return getCustomItemServiceInfo(player, block)
                ?: providers.asSequence()
                    .filterIsInstance<VanillaWailaInfoProvider>()
                    .lastOrNull { it.materials == null || type in it.materials }
                    ?.getInfo(player, block)
        }
    }
    
    private fun getCustomItemServiceInfo(player: Player, block: Block): WailaInfo? {
        val blockId = CustomItemServiceManager.getId(block)?.let { runCatching { NamespacedId.of(it) }.getOrNull() } ?: return null
        val blockName = CustomItemServiceManager.getName(block, player.locale) ?: return null
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(ComponentBuilder().append(blockName).create(), WailaLine.Alignment.CENTERED)
        lines += WailaLine(ComponentBuilder(blockId.toString()).color(ChatColor.DARK_GRAY).create(), WailaLine.Alignment.CENTERED)
        lines += ToolLine.getCustomItemServiceToolLine(player, block)
        
        return WailaInfo(blockId, lines)
    }
    
}