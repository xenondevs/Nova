package xyz.xenondevs.nova.ui.overlay.character.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.builder.content.font.FontChar
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.parseResourceLocation

class GuiTexture private constructor(private val info: FontChar) {
    
    val component = info.component
        .color(NamedTextColor.WHITE)
    
    fun getTitle(translate: String): Component {
        return getTitle(Component.translatable(translate))
    }
    
    fun getTitle(title: Component): Component {
        return Component.text()
            .move(-8)
            .append(component)
            .move(-info.width + 7)
            .append(title)
            .build()
    }
    
    companion object {
        
        internal fun of(id: String) = GuiTexture(NovaRegistries.GUI_DATA_LOOKUP.getOrThrow(id))
        
        internal fun of(id: ResourceLocation) = GuiTexture(NovaRegistries.GUI_DATA_LOOKUP.getOrThrow(id))
        
        fun of(addon: Addon, name: String) = of(parseResourceLocation(name, addon.description.id))
        
    }
    
}