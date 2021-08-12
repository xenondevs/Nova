package xyz.xenondevs.nova.overlay.impl

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.overlay.ActionbarOverlay
import xyz.xenondevs.nova.overlay.CustomCharacters

class JetpackOverlay : ActionbarOverlay {
    
    override var text: Array<BaseComponent> = getCurrentText()
        private set
    
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
            .append(CustomCharacters.getMovingComponent(216))
            .append(('\uF000'.code + stage).toChar().toString())
            .font("nova:jetpack")
            .create()
    }
    
}