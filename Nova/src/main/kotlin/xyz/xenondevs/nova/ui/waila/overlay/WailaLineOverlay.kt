package xyz.xenondevs.nova.ui.waila.overlay

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.overlay.bossbar.CenteredTextBossBarOverlay

internal class WailaLineOverlay(player: Player, line: Int) : CenteredTextBossBarOverlay(player) {
    
    override val y = -13 - line * 12
    
    override var text: Array<BaseComponent> = arrayOf(TextComponent(""))
    override var centerX = 0
    override var width = 0
    
}