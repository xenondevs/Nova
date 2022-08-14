package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.util.data.toPlainText

abstract class CenteredTextBossBarOverlay(private val player: Player) : BossBarOverlay() {
    
    abstract val text: Array<BaseComponent>
    abstract val y: Int
    
    final override val barLevel: Int
        get() = y / 19 * -1
    
    final override val components: Array<BaseComponent>
        get() = DefaultFont.getVerticallyMovedText(text, y % 19)
    
    override val width: Int
        get() = DefaultFont.getStringLength(text.toPlainText(player.locale))
    
}