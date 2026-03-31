package xyz.xenondevs.nova.registry

import net.kyori.adventure.text.Component
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition.Alignment

/**
 * A builder for [GuiTexture].
 */
@RegistryElementBuilderDsl
sealed interface GuiTextureBuilder : RegistryEntryBuilder.Nova<GuiTexture> {
    
    /**
     * Enables or disables the "Inventory" text that is displayed above the player's inventory slots.
     * Defaults to `true`, meaning the text is shown.
     */
    fun inventoryLabel(inventoryLabel: Boolean)
    
    /**
     * Configures the title text and its alignment.
     */
    fun title(title: GuiTextureTitleBuilder.() -> Unit)
    
    /**
     * Configures the texture.
     */
    fun texture(texture: GuiTextureLayoutBuilder.() -> Unit)
    
}

/**
 * A builder for the title of a [GuiTexture], consisting of one or more lines.
 * A [GuiTexture's][GuiTexture] title can be both:
 * * static: defined via [line], retrieved via [GuiTexture.getTitle], or
 * * dynamic: only alignment is defined via [alignment], actual text is set in [GuiTexture.getTitle].
 */
@RegistryElementBuilderDsl
sealed interface GuiTextureTitleBuilder {
    
    /**
     * Sets the positioning of the title text used in [GuiTexture.getTitle].
     */
    fun alignment(
        alignment: Alignment = Alignment.DEFAULT,
        offset: Vector2ic = Vector2i(0, 0)
    )
    
    /**
     * Adds a line of text to the default title, additional to any custom title
     * text set in [GuiTexture.getTitle].
     * Can be invoked multiple times to add multiple lines.
     */
    fun line(
        text: Component,
        alignment: Alignment = Alignment.DEFAULT,
        offset: Vector2ic = Vector2i(0, 0)
    )
    
}