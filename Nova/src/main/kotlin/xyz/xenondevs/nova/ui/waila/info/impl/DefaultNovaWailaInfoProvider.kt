package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.util.data.localized

internal object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: NovaBlockState): WailaInfo {
        val material = block.material
        
        return WailaInfo(
            material.id,
            listOf(
                arrayOf(localized(ChatColor.WHITE, material.localizedName)),
                ComponentBuilder(material.id.toString()).color(ChatColor.DARK_PURPLE).create()
            ),
            player
        )
    }
    
}