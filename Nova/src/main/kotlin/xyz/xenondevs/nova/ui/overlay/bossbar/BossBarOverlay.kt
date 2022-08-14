package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont

abstract class BossBarOverlay {
    
    /**
     * Which boss bar should be used. For every bar level, the text will be displayed 20px deeper.
     */
    abstract val barLevel: Int
    
    /**
     * The components to display.
     */
    abstract val components: Array<BaseComponent>
    
    /**
     * The width of the [components] in pixels.
     * @see [DefaultFont.getStringLength]
     */
    abstract val width: Int
    
    /**
     * At which x-coordinate the [text][components] should be centered at.
     * Null if there should they shouldn't be centered.
     */
    abstract val centerX: Int?
    
    /**
     * If the [components] have been changed and an update should be sent in the next tick.
     */
    var changed: Boolean = false
    
}