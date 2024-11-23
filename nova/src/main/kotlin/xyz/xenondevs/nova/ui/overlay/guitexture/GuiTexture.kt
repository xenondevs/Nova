package xyz.xenondevs.nova.ui.overlay.guitexture

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart

class GuiTexture internal constructor(
    val id: Key,
    internal val makeLayout: (ResourcePackBuilder) -> GuiTextureLayout
) {
    
    val component: Component by lazy {
        val data = ResourceLookups.GUI_TEXTURE[this]!!
        Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint)).font(data.font))
            .build()
    }
    
    fun getTitle(translate: String): Component =
        getTitle(Component.translatable(translate))
    
    fun getTitle(title: Component): Component =
        Component.text()
            .append(component)
            .moveToStart()
            .append(title)
            .build()
    
}