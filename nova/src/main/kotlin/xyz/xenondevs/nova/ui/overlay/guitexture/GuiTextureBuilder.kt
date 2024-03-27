package xyz.xenondevs.nova.ui.overlay.guitexture

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.layout.gui.GuiTextureLayout
import xyz.xenondevs.nova.data.resources.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder

class GuiTextureBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<GuiTexture>(NovaRegistries.GUI_TEXTURE, id) {
    
    private var color: TextColor = NamedTextColor.WHITE
    private var layout: GuiTextureLayout? = null
    
    fun color(color: TextColor) {
        this.color = color
    }
    
    fun texture(texture: GuiTextureLayoutBuilder.() -> Unit) {
        layout = GuiTextureLayoutBuilder(id.namespace).apply(texture).build()
    }
    
    override fun build(): GuiTexture =
        GuiTexture(id, color, layout ?: throw IllegalStateException("No layout"))
    
}