package xyz.xenondevs.nova.ui.overlay.impl

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.ui.overlay.ActionbarOverlay
import xyz.xenondevs.nova.ui.overlay.MoveCharacters

class JetpackOverlay : ActionbarOverlay {
    
    override var text: Array<BaseComponent> = getCurrentText()
        private set
    
    // 95 is the moved distance, 23 is texture size + 1
    override val width = 95 + 23
    
    var percentage: Double = 0.0
        set(value) {
            require(value in 0.0..1.0)
            if (field == value) return
            field = value
            text = getCurrentText()
        }
    
    private fun getCurrentText(): Array<BaseComponent> {
        val stage = (percentage * 38).toInt()
        
        return ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(95))
            .append(('\uF000'.code + stage).toChar().toString())
            .font("nova:jetpack")
            .create()
    }
    
}