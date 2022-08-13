package xyz.xenondevs.nova.ui.overlay.character.gui

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.data.formatWithTemplate

private val TITLE_TEMPLATE = ComponentBuilder("")
    .color(ChatColor.DARK_GRAY)
    .font("default")
    .create()[0]

class GUITexture(private val data: GUIData) {
    
    val component: BaseComponent = ComponentBuilder(data.char.toString())
        .font("nova:gui")
        .color(ChatColor.WHITE)
        .create()[0]
    
    fun getTitle(translate: String): Array<BaseComponent> {
        return getTitle(TranslatableComponent(translate))
    }
    
    fun getTitle(title: BaseComponent): Array<BaseComponent> {
        return getTitle(arrayOf(title))
    }
    
    fun getTitle(title: Array<BaseComponent>): Array<BaseComponent> {
        return ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(-8)) // move to side to place overlay
            .append(data.char.toString())
            .font("nova:gui")
            .color(ChatColor.WHITE)
            .append(MoveCharacters.getMovingComponent(-data.width + 7)) // move back to start
            .append(title.formatWithTemplate(TITLE_TEMPLATE))
            .create()
    }
    
    companion object {
        
        internal fun of(id: String) = GUITexture(Resources.getGUIData(id))
        
        fun of(addon: Addon, name: String) = of(name.addNamespace(addon.description.id))
        
    }
    
}