package xyz.xenondevs.nova.ui.overlay.guitexture

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart

class GuiTexture internal constructor(
    val id: ResourceLocation,
    val color: TextColor,
    internal val layout: GuiTextureLayout
) {
    
    val component: Component by lazy { 
        val data = ResourceLookups.GUI_TEXTURE[this]!!
        Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), color).font(data.font))
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