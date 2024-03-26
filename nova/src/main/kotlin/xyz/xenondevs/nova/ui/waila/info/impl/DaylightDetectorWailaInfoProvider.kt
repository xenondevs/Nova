package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.DaylightDetector
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object DaylightDetectorWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.DAYLIGHT_DETECTOR)) {
    
    override fun getInfo(player: Player, pos: BlockPos, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, block)
        val detector = block.blockData as DaylightDetector
        if (detector.isInverted) {
            info.icon = ResourceLocation("minecraft", "daylight_detector_inverted")
        }
        return info
    }
    
}