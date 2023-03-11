package xyz.xenondevs.nova.ui.overlay.character.gui

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.content.font.FontChar
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.component.bungee.formatWithTemplate

private val TITLE_TEMPLATE = ComponentBuilder("")
    .color(ChatColor.DARK_GRAY)
    .font("default")
    .create()[0]

class GuiTexture(private val info: FontChar) {
    
    val component: BaseComponent = ComponentBuilder(info.char.toString())
        .font(info.font)
        .color(ChatColor.WHITE)
        .create()[0]
    
    fun getTitle(translate: String): Array<out BaseComponent> {
        return getTitle(TranslatableComponent(translate))
    }
    
    fun getTitle(title: BaseComponent): Array<out BaseComponent> {
        return getTitle(arrayOf(title))
    }
    
    fun getTitle(title: Array<out BaseComponent>): Array<out BaseComponent> {
        return ComponentBuilder()
            .append(MoveCharacters.getMovingBungeeComponent(-8)) // move to side to place overlay
            .append(info.bungeeComponent)
            .append(MoveCharacters.getMovingBungeeComponent(-info.width + 7)) // move back to start
            .append(title.formatWithTemplate(TITLE_TEMPLATE))
            .create()
    }
    
    companion object {
        
        internal fun of(id: String) = GuiTexture(Resources.getGuiChar(id))
        
        fun of(addon: Addon, name: String) = of(name.addNamespace(addon.description.id))
        
    }
    
}