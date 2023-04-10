package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.SuspiciousSand
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object SuspiciousSandWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.SUSPICIOUS_SAND)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val sand = block.blockData as SuspiciousSand
        info.icon = ResourceLocation("minecraft", "suspicious_sand_${sand.dusted}")
        return info
    }
    
}