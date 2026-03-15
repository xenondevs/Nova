package xyz.xenondevs.nova.ui.overlay.guitexture

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.ksp.annotation.GenerateFlatMapExtensions
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureSerializer
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.util.component.adventure.isEmpty
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.toMinecraftLocaleCode
import java.util.*
import kotlin.math.roundToInt

@GenerateFlatMapExtensions
@Serializable(with = GuiTextureSerializer::class)
class GuiTexture internal constructor(
    override val entry: RegistryEntry.Nova<GuiTexture>,
    private val data: Provider<GuiTextureData>,
    /**
     * The position of the title text added via [getTitle].
     */
    val titlePosition: TitlePosition,
    /**
     * Additional title text lines and their position that are always there.
     */
    val extraLines: List<Pair<Component, TitlePosition>>,
    /**
     * Whether the inventory label (the title of the player's inventory) should be shown.
     */
    val hasInventoryLabel: Boolean
) : NovaRegistryElement<GuiTexture> {
    
    /**
     * The provider of component of the raw gui texture with no title text.
     */
    val component: Provider<Component> = data.map { data ->
        Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), NamedTextColor.WHITE).font(data.font))
            .build()
    }
    
    /**
     * Gets a provider of the gui texture component with all [extraLines] for [locale].
     */
    fun getTitle(locale: Provider<Locale> = provider(Locale.US)): Provider<Component> =
        getTitle(Component.empty(), locale)
    
    /**
     * Gets a provider of the gui texture component with [translate] as the title text
     * at [titlePosition] and all [extraLines] for [locale].
     */
    fun getTitle(
        translate: String,
        locale: Provider<Locale> = provider(Locale.US)
    ): Provider<Component> = getTitle(Component.translatable(translate), locale)
    
    /**
     * Gets a provider of the gui texture component with [title] as the title text
     * at [titlePosition] and all [extraLines] for [locale].
     */
    fun getTitle(
        title: Component,
        locale: Provider<Locale> = provider(Locale.US)
    ): Provider<Component> = combinedProvider(data, locale) { data, locale ->
        val builder = Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), NamedTextColor.WHITE).font(data.font))
            .move(-data.width - 1)
        
        sequenceOf(title to titlePosition, *extraLines.toTypedArray())
            .filterNot { (text, _) -> text.isEmpty(locale) }
            // render server-side to prevent client-side translation mismatch from impacting alignment
            .map { (text, position) -> LocaleManager.render(text, locale) to position }
            .forEach { (text, position) ->
                val textWidth = CharSizes.calculateComponentWidth(text, locale.toMinecraftLocaleCode()).roundToInt()
                when (position.alignment) {
                    TitlePosition.Alignment.DEFAULT -> {
                        builder
                            .move(-data.offset + position.offset.x())
                            .append(MovedFonts.moveVertically(text, position.offset.y()))
                            .move(-textWidth - position.offset.x() + data.offset)
                    }
                    
                    TitlePosition.Alignment.LEFT -> {
                        builder
                            .move(position.offset.x())
                            .append(MovedFonts.moveVertically(text, position.offset.y()))
                            .move(-textWidth - position.offset.x())
                    }
                    
                    TitlePosition.Alignment.CENTER -> {
                        builder
                            .move((data.width / 2f - textWidth / 2f + position.offset.x()).roundToInt())
                            .append(MovedFonts.moveVertically(text, position.offset.y()))
                            .move((-position.offset.x() - textWidth / 2f - data.width / 2f).roundToInt())
                    }
                    
                    TitlePosition.Alignment.RIGHT -> {
                        builder
                            .move(data.width + 1 - textWidth + position.offset.x())
                            .append(MovedFonts.moveVertically(text, position.offset.y()))
                            .move(-data.width - 1 - position.offset.x())
                    }
                }
            }
        
        builder.build()
    }
    
    /**
     * The position of a title text in a [GuiTexture].
     */
    data class TitlePosition(
        /**
         * The alignment of the title relative to the gui texture.
         */
        val alignment: Alignment = Alignment.DEFAULT,
        /**
         * An additional offset to apply to the text.
         */
        val offset: Vector2ic = Vector2i(0, 0)
    ) {
        
        /**
         * Horizontal alignment of a title text line.
         */
        enum class Alignment {
            
            /**
             * The default horizontal position of the title text line in the given menu type.
             */
            DEFAULT,
            
            /**
             * Horizontally aligned to the left edge of the gui texture,
             * such that the leftmost pixels of the text overlap with the leftmost pixels of the gui texture.
             */
            LEFT,
            
            /**
             * Horizontally aligned to the center of the gui texture.
             */
            CENTER,
            
            /**
             * Horizontally aligned to the right edge of the gui texture,
             * such that the rightmost pixels of the text overlap with the rightmost pixels of the gui texture.
             */
            RIGHT
            
        }
    }
    
    override fun toString(): String = key.toString()
    
}