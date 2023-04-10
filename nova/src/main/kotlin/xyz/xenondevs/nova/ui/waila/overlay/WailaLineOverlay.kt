package xyz.xenondevs.nova.ui.waila.overlay

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.util.component.adventure.move

internal class WailaLineOverlay(line: Int) : BossBarOverlay {
    
    override val offset = 13 + line * 12
    
    override val component: Component
        get() = if (!centered)
            Component.text()
                .move(x)
                .append(text)
                .build()
        else text
    
    override val centerX: Int?
        get() = if (centered) x else null
    
    var text: Component = Component.empty()
    var x = 0
    var centered = false
    
    fun clear() {
        if (text !== Component.empty() || x != 0) {
            text = Component.empty()
            x = 0
        }
    }
    
}