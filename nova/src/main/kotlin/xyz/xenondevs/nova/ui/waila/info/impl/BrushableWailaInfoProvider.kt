package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Brushable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object BrushableWailaInfoProvider : VanillaWailaInfoProvider<Brushable>(
    setOf(Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL)
) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Brushable): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = ResourceLocation("minecraft", "${blockState.material.name.lowercase()}_${blockState.dusted}")
        return info
    }
    
}