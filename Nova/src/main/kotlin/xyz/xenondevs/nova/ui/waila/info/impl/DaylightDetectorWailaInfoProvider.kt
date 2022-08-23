package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.DaylightDetector
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object DaylightDetectorWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.DAYLIGHT_DETECTOR)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val detector = block.blockData as DaylightDetector
        if (detector.isInverted) {
            info.icon = NamespacedId("minecraft", "daylight_detector_inverted")
        }
        return info
    }
    
}