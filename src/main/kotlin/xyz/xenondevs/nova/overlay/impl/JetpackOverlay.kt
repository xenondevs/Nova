package xyz.xenondevs.nova.overlay.impl

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.overlay.ActionbarOverlay
import xyz.xenondevs.nova.overlay.CustomCharacters

class JetpackOverlay : ActionbarOverlay {
    
    override fun getText(): Array<BaseComponent> {
        return ComponentBuilder()
            .append(CustomCharacters.getMovingComponent(50))
            .append(CustomCharacters.JETPACK_FUEL.component)
            .append("                     ")
            .font("default")
            .create()
    }
    
}