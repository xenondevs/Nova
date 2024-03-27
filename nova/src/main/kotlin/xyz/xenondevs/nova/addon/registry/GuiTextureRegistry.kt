package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTextureBuilder
import xyz.xenondevs.nova.util.ResourceLocation

interface GuiTextureRegistry : AddonGetter {
    
    fun guiTexture(name: String, texture: GuiTextureBuilder.() -> Unit): GuiTexture =
        GuiTextureBuilder(ResourceLocation(addon, name)).apply(texture).register()
    
}