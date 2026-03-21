package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture

/**
 * A builder for [GuiTexture].
 */
@RegistryElementBuilderDsl
sealed interface GuiTextureBuilder {
    
    /**
     * Enables or disables the "Inventory" text that is displayed above the player's inventory slots.
     * Defaults to `true`, meaning the text is shown.
     */
    fun inventoryLabel(inventoryLabel: Boolean)
    
    /**
     * Configures the texture.
     */
    fun texture(texture: GuiTextureLayoutBuilder.() -> Unit)
    
}