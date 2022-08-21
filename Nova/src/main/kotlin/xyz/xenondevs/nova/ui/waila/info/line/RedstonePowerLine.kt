package xyz.xenondevs.nova.ui.waila.info.line

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.Block
import org.bukkit.block.data.Powerable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder

object RedstonePowerLine {
    
    fun getRedstonePowerLine(player: Player, block: Block): WailaLine {
        val powerable = block.blockData as Powerable
        
        return WailaLine(
            ComponentWidthBuilder(player.locale)
                .append(TranslatableComponent("waila.nova.redstone_power." + if (powerable.isPowered) "on" else "off"))
                .color(if (powerable.isPowered) ChatColor.GREEN else ChatColor.RED)
                .create(),
            Alignment.CENTERED
        )
    }
    
}