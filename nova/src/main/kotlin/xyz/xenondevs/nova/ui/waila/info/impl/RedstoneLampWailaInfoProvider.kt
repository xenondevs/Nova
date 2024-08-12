package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object RedstoneLampWailaInfoProvider : VanillaWailaInfoProvider<Lightable>(setOf(Material.REDSTONE_LAMP)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Lightable): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        if (blockState.isLit) {
            info.icon = ResourceLocation.withDefaultNamespace("redstone_lamp_on")
        }
        
        return info
    }
    
}