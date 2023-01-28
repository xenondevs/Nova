package xyz.xenondevs.nova.ui.waila.overlay

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.ui.overlay.character.MovedFonts
import xyz.xenondevs.nova.util.data.MovingComponentBuilder

private val EMPTY_TEXT = arrayOf(TextComponent(""))

internal class WailaLineOverlay(line: Int) : BossBarOverlay() {
    
    private val y = -13 - line * 12
    
    override val barLevel = y / 19 * -1
    override val components: Array<out BaseComponent>
        get() {
            return if (centered)
                MovedFonts.moveVertically(this.text, y % 19)
            else MovingComponentBuilder()
                .move(x) 
                .append(MovedFonts.moveVertically(this.text, y % 19))
                .create()
        }
    override val centerX: Int?
        get() = if (centered) x else null
    
    var text: Array<out BaseComponent> = EMPTY_TEXT
    var x = 0
    var centered = false
    
    fun clear() {
        if (text !== EMPTY_TEXT || x != 0) {
            text = EMPTY_TEXT
            x = 0
            
            changed = true
        }
    }
    
}