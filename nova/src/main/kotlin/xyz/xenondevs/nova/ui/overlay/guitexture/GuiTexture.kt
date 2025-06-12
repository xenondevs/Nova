package xyz.xenondevs.nova.ui.overlay.guitexture

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureSerializer
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart

@Serializable(with = GuiTextureSerializer::class)
class GuiTexture internal constructor(
    val id: Key,
    internal val makeLayout: (ResourcePackBuilder) -> GuiTextureLayout
) {
    
    val component: Component by ResourceLookups.GUI_TEXTURE_LOOKUP.getProvider(this).map { data ->
        checkNotNull(data)
        Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), NamedTextColor.WHITE).font(data.font))
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