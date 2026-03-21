package xyz.xenondevs.nova.ui.overlay.guitexture

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureSerializer
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart

val Provider<GuiTexture>.component: Provider<Component>
    get() = flatMap(GuiTexture::component)

fun Provider<GuiTexture>.getTitle(translate: String): Provider<Component> =
    flatMap { it.getTitle(translate) }

fun Provider<GuiTexture>.getTitle(title: Component): Provider<Component> =
    flatMap { it.getTitle(title) }

@Serializable(with = GuiTextureSerializer::class)
class GuiTexture internal constructor(
    override val entry: RegistryEntry.Nova<GuiTexture>,
    data: Provider<GuiTextureData>,
    val hasInventoryLabel: Boolean
) : NovaRegistryElement<GuiTexture> {
    
    val component: Provider<Component> = data.map { data ->
        checkNotNull(data)
        Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), NamedTextColor.WHITE).font(data.font))
            .build()
    }
    
    fun getTitle(translate: String): Provider<Component> =
        getTitle(Component.translatable(translate))
    
    fun getTitle(title: Component): Provider<Component> = component.map {
        Component.text()
            .append(component.get())
            .moveToStart()
            .append(title)
            .build()
    }
    
    override fun toString(): String = key.toString()
    
}