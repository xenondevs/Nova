package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont

abstract class CenteredTextBossBarOverlay : BossBarOverlay() {
    
    abstract val text: Array<out BaseComponent>
    abstract val y: Int
    
    final override val barLevel: Int
        get() = y / 19 * -1
    
    final override val components: Array<out BaseComponent>
        get() = DefaultFont.getVerticallyMovedText(text, y % 19)
    
}