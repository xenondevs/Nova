package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Repeater
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder

object RepeaterWailaInfoProvider : VanillaWailaInfoProvider(listOf(Material.REPEATER)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        
        val repeater = block.blockData as Repeater
        info.icon = getIconName(repeater)
        
        info.lines += WailaLine(
            ComponentWidthBuilder(player.locale)
                .append(TranslatableComponent("waila.nova.repeater.delay", repeater.delay))
                .color(ChatColor.GRAY)
                .create(),
            Alignment.CENTERED
        )
        
        if (repeater.isLocked) {
            info.lines += WailaLine(
                ComponentWidthBuilder(player.locale)
                    .append(TranslatableComponent("waila.nova.repeater.locked"))
                    .color(ChatColor.RED)
                    .create(),
                Alignment.CENTERED
            )
        }
        
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