package xyz.xenondevs.nova.ui.overlay

import net.md_5.bungee.api.chat.BaseComponent

interface ActionbarOverlay {
    
    val text: Array<BaseComponent>
    
    val width: Int
    
}