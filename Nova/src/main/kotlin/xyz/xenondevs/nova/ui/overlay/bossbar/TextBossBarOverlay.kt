package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.data.toPlainText

abstract class TextBossBarOverlay(private val player: Player) : BossBarOverlay() {
    
    abstract val text: Array<BaseComponent>
    abstract val x: Int
    abstract val y: Int
    
    final override val barLevel: Int
        get() = y / 20 * -1
    
    final override val components: Array<BaseComponent>
        get() {
            return arrayOf(MoveCharacters.getMovingComponent(x)) +
                DefaultFont.getVerticallyMovedText(text, y % 20)
        }
    
    final override val width: Int
        get() = x + DefaultFont.getStringLength(text.toPlainText(player.locale))
    
}