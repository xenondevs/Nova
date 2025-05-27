package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture

@Deprecated(REGISTRIES_DEPRECATION)
interface GuiTextureRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun guiTexture(name: String, texture: GuiTextureLayoutBuilder.() -> Unit): GuiTexture =
        addon.guiTexture(name, texture)
    
}