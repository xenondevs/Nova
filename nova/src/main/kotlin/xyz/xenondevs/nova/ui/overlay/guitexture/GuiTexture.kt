package xyz.xenondevs.nova.ui.overlay.guitexture

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.bootstrapFlatMap
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureSerializer
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.util.component.adventure.isEmpty
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.toMinecraftLocaleCode
import java.util.*

/**
 * Shortcut to [bootstrapFlatMap][bootstrapFlatMap] to [GuiTexture.component].
 */
val Provider<GuiTexture>.component: Provider<Component>
    get() = bootstrapFlatMap { it.component }

/**
 * Shortcut to [bootstrapFlatMap][bootstrapFlatMap] to [GuiTexture.getTitle].
 */
fun Provider<GuiTexture>.getTitle(locale: Provider<Locale>): Provider<Component> =
    bootstrapFlatMap { it.getTitle(locale) }

/**
 * Shortcut to [bootstrapFlatMap][bootstrapFlatMap] to [GuiTexture.getTitle].
 */
fun Provider<GuiTexture>.getTitle(translate: String, locale: Provider<Locale>): Provider<Component> =
    bootstrapFlatMap { it.getTitle(translate, locale) }

/**
 * Shortcut to [bootstrapFlatMap][bootstrapFlatMap] to [GuiTexture.getTitle].
 */
fun Provider<GuiTexture>.getTitle(lines: List<Component>, locale: Provider<Locale>): Provider<Component> =
    bootstrapFlatMap { it.getTitle(lines, locale) }

@Serializable(with = GuiTextureSerializer::class)
class GuiTexture internal constructor(
    override val entry: RegistryEntry.Nova<GuiTexture>,
    private val data: Provider<GuiTextureData>,
    private val titleLines: List<TitleLine>,
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
     * Gets a provider of the gui texture component with all static lines for [locale].
     */
    fun getTitle(locale: Provider<Locale>): Provider<Component> =
        getTitle(emptyList(), locale)
    
    /**
     * Gets a provider of the gui texture component with [translate] in the first dynamic line
     * and all static lines for [locale].
     */
    fun getTitle(translate: String, locale: Provider<Locale>): Provider<Component> =
        getTitle([Component.translatable(translate)], locale)
    
    /**
     * Gets a provider of the gui texture component with all static lines and [lines] filling the
     * dynamic lines in the order they are defined.
     */
    fun getTitle(lines: List<Component>, locale: Provider<Locale>): Provider<Component> = combinedProvider(data, locale) { data, locale ->
        val builder = Component.text()
            .move(data.offset)
            .append(Component.text(Character.toString(data.codePoint), NamedTextColor.WHITE).font(data.font))
            .move(-data.width - 1)
        
        var dynamicLineIndex = 0
        titleLines.asSequence()
            .map { line ->
                val text = when (line) {
                    is TitleLine.Static -> line.text
                    is TitleLine.Dynamic -> lines.getOrNull(dynamicLineIndex++) ?: Component.empty()
                }
                text to line.position
            }
            .filterNot { [text, _] -> text.isEmpty(locale) }
            // render server-side to prevent client-side translation mismatch from impacting alignment
            .map { [text, position] -> LocaleManager.render(text, locale) to position }
            .forEach { [text, position] ->
                val movedText = MovedFonts.moveVertically(text, position.offset.y())
                val textSize = CharSizes.calculateComponentSize(movedText, locale.toMinecraftLocaleCode(), false)
                when (position.alignment) {
                    TitlePosition.Alignment.DEFAULT -> {
                        val preMove = -data.offset + position.offset.x()
                        builder
                            .move(preMove)
                            .append(movedText)
                            .move(-textSize.width - preMove)
                    }
                    
                    TitlePosition.Alignment.LEFT -> {
                        val preMove = position.offset.x() - textSize.xRange.start
                        builder
                            .move(preMove)
                            .append(movedText)
                            .move(-textSize.width - preMove)
                    }
                    
                    TitlePosition.Alignment.CENTER -> {
                        val visualCenter = (textSize.xRange.start + textSize.xRange.endInclusive) / 2
                        val preMove = data.width / 2f + position.offset.x() - visualCenter
                        builder
                            .move(preMove)
                            .append(movedText)
                            .move(-textSize.width - preMove)
                    }
                    
                    TitlePosition.Alignment.RIGHT -> {
                        val preMove = data.width + 1 + position.offset.x() - textSize.xRange.endInclusive
                        builder
                            .move(preMove)
                            .append(movedText)
                            .move(-textSize.width - preMove)
                    }
                }
            }
        
        builder.build()
    }
    
    internal sealed interface TitleLine {
        
        val position: TitlePosition
        
        data class Static(
            override val position: TitlePosition,
            val text: Component
        ) : TitleLine
        
        data class Dynamic(
            override val position: TitlePosition,
            val fonts: Set<Key>
        ) : TitleLine
        
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
    
    override fun toString(): String = key.asString()
    
}
