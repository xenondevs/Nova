package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine

object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: NovaBlockState): WailaInfo {
        val material = block.material
        
        val translate = TranslatableComponent(material.localizedName)
        translate.color = ChatColor.WHITE
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(ComponentBuilder().append(translate).color(ChatColor.WHITE).create(), player, WailaLine.Alignment.CENTERED)
        lines += WailaLine(ComponentBuilder(material.id.toString()).color(ChatColor.DARK_GRAY).create(), player, WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, material)
        
        return WailaInfo(material.id, lines)
    }
    
}