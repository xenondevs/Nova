package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Repeater
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object RepeaterWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.REPEATER)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val repeater = block.blockData as Repeater
        info.icon = getIconName(repeater)
        return info
    }
    
    private fun getIconName(repeater: Repeater): NamespacedId {
        return NamespacedId(
            "minecraft",
            "repeater_${repeater.delay}tick"
                + (if (repeater.isPowered) "_on" else "")
                + (if (repeater.isLocked) "_locked" else "")
        )
    }
    
}