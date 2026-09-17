package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
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
     * Configures the title lines.
     * If this function is not called, the title consists of one [dynamic line][GuiTextureTitleBuilder.dynamicLine]
     * at the default position.
     */
    fun title(title: GuiTextureTitleBuilder.() -> Unit)
    
    /**
     * Configures the texture.
     */
    fun texture(texture: GuiTextureLayoutBuilder.() -> Unit)
    
}

/**
 * A builder for the title of a [GuiTexture], consisting of static and dynamic lines.
 * Static lines are defined during registration, while the contents of dynamic lines are supplied to
 * [GuiTexture.getTitle] in declaration order.
 */
@RegistryElementBuilderDsl
sealed interface GuiTextureTitleBuilder {
    
    /**
     * Adds a static title line whose [text] is defined during registration.
     */
    fun staticLine(
        text: Component,
        alignment: Alignment = Alignment.DEFAULT,
        offset: Vector2ic = Vector2i(0, 0)
    )
    
    /**
     * Adds a dynamic title line whose contents are supplied to [GuiTexture.getTitle].
     *
     * [fonts] should list all fonts that may be used during runtime. Vertically moved variants
     * will be automatically generated for them.
     */
    fun dynamicLine(
        alignment: Alignment = Alignment.DEFAULT,
        offset: Vector2ic = Vector2i(0, 0),
        fonts: Set<Key> = setOf(Key.key("minecraft", "default"))
    )
    
}
