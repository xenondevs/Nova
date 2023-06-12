package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Brushable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object BrushableWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val brushable = block.blockData as Brushable
        info.icon = ResourceLocation("minecraft", "${block.type.name.lowercase()}_${brushable.dusted}")
        return info
    }
    
}