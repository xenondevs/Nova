package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Bell
import org.bukkit.block.data.type.Bell.Attachment
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object BellWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.BELL)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val bell = block.blockData as Bell
        val name = when (bell.attachment) {
            Attachment.CEILING -> "bell_ceiling"
            Attachment.DOUBLE_WALL -> "bell_between_walls"
            Attachment.FLOOR -> "bell_floor"
            Attachment.SINGLE_WALL -> "bell_wall"
        }
        info.icon = NamespacedId("minecraft", name)
        return info
    }
    
}