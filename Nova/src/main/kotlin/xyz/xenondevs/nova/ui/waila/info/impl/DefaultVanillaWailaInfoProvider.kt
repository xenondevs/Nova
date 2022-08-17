package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.util.item.localizedName

internal object DefaultVanillaWailaInfoProvider : VanillaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val material = block.type
        val id = NamespacedId("minecraft", material.name.lowercase())
        
        val translate = material.localizedName?.let(::TranslatableComponent) ?: TextComponent(material.name)
        translate.color = ChatColor.WHITE
        
        return WailaInfo(
            id,
            listOf(
                ComponentBuilder().append(translate).color(ChatColor.WHITE).create() to null,
                ComponentBuilder(id.toString()).color(ChatColor.DARK_GRAY).create() to null,
                ToolLine.getToolLine(player, block)
            ),
            player
        )
    }
    
}