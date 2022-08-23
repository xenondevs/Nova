package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.RedstoneRail
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object RailWailaInfoProvider : VanillaWailaInfoProvider(
    setOf(Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL)
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val defaultInfo = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val rail = block.blockData as RedstoneRail
        defaultInfo.icon = NamespacedId("minecraft", block.type.name.lowercase() + if (rail.isPowered) "_on" else "")
        return defaultInfo
    }
    
}