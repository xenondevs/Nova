package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters

abstract class TextBossBarOverlay : BossBarOverlay() {
    
    final override val centerX = null
    abstract val text: Array<out BaseComponent>
    abstract val x: Int
    abstract val y: Int
    
    final override val barLevel: Int
        get() = y / 19 * -1
    
    final override val components: Array<BaseComponent>
        get() {
            return arrayOf(MoveCharacters.getMovingComponent(x)) +
                DefaultFont.getVerticallyMovedText(text, y % 19)
        }
    
}