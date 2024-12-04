package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.Repeater
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object RepeaterWailaInfoProvider : VanillaWailaInfoProvider<Repeater>(setOf(Material.REPEATER)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Repeater): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = getIconName(blockState)
        return info
    }
    
    private fun getIconName(repeater: Repeater): Key {
        return Key.key(
            "repeater_${repeater.delay}tick"
                + (if (repeater.isPowered) "_on" else "")
                + (if (repeater.isLocked) "_locked" else "")
        )
    }
    
}