package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.data.Key
import xyz.xenondevs.nova.util.set

interface GuiTextureRegistry : AddonHolder {
    
    fun guiTexture(name: String, texture: GuiTextureLayoutBuilder.() -> Unit): GuiTexture {
        val id = Key(addon, name)
        val texture = GuiTexture(id) { GuiTextureLayoutBuilder(id.namespace(), it).apply(texture).build() }
        NovaRegistries.GUI_TEXTURE[id] = texture
        return texture
    }
    
}