@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.util.component.adventure

import com.mojang.serialization.JsonOps
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.FontDescription
import org.bukkit.entity.Player
import xyz.xenondevs.invui.internal.util.ComponentUtils
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.ui.overlay.MoveCharacters
import xyz.xenondevs.nova.util.toIdentifier
import java.awt.Color
import java.util.*
import net.minecraft.network.chat.Component as MojangComponent
import net.minecraft.network.chat.Style as MojangStyle

fun String.toAdventureComponent(): Component {
    return GsonComponentSerializer.gson().deserialize(this)
}

fun String.toAdventureComponentOrNull(): Component? {
    return runCatching { GsonComponentSerializer.gson().deserialize(this) }.getOrNull()
}

fun String.toAdventureComponentOrEmpty(): Component {
    return toAdventureComponentOrNull() ?: Component.empty()
}

fun String.toAdventureComponentOr(createComponent: () -> Component): Component {
    return toAdventureComponentOrNull() ?: createComponent()
}

fun MojangComponent.toAdventureComponent(): Component {
    return PaperAdventure.asAdventure(this)
}

fun Array<out BaseComponent>.toAdventureComponent(): Component {
    return ComponentSerializer.toString(this).toAdventureComponent()
}

fun MojangComponent.toJson(): String {
    return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow().toString()
}

fun Component.toNMSComponent(): MojangComponent {
    return PaperAdventure.asVanilla(this)
}

fun Component.toJson(): String {
    return GsonComponentSerializer.gson().serialize(this)
}

fun Component.toNBT(): StringTag {
    return StringTag.valueOf(toJson())
}

fun Component.toPlainText(player: Player): String {
    return PlainTextComponentConverter.toPlainText(this, player.locale)
}

fun Component.toPlainText(locale: String = "en_us"): String {
    return PlainTextComponentConverter.toPlainText(this, locale)
}

fun Component.font(font: String): Component {
    return font(Key.key(font))
}

fun Component.fontName(): String? {
    return font()?.toString()
}

fun Component.withoutPreFormatting(): Component {
    return ComponentUtils.withoutPreFormatting(this)
}

private val DEFAULT_STYLE = MojangStyle.EMPTY
    .withColor(0xFFFFFF)
    .withBold(false)
    .withItalic(false)
    .withUnderlined(false)
    .withStrikethrough(false)
    .withObfuscated(false)

fun MojangComponent.withoutPreFormatting(): MojangComponent {
    return MojangComponent.literal("")
        .withStyle(DEFAULT_STYLE)
        .append(this)
}

fun Style.toNmsStyle(): MojangStyle {
    var style = MojangStyle.EMPTY
    color()?.let { style = style.withColor(it.value()) }
    font()?.let { style = style.withFont(FontDescription.Resource(it.toIdentifier())) }
    
    when (decoration(TextDecoration.BOLD)) {
        TextDecoration.State.TRUE -> style = style.withBold(true)
        TextDecoration.State.FALSE -> style = style.withBold(false)
        else -> Unit
    }
    
    when (decoration(TextDecoration.ITALIC)) {
        TextDecoration.State.TRUE -> style = style.withItalic(true)
        TextDecoration.State.FALSE -> style = style.withItalic(false)
        else -> Unit
    }
    
    when (decoration(TextDecoration.UNDERLINED)) {
        TextDecoration.State.TRUE -> style = style.withUnderlined(true)
        TextDecoration.State.FALSE -> style = style.withUnderlined(false)
        else -> Unit
    }
    
    when (decoration(TextDecoration.STRIKETHROUGH)) {
        TextDecoration.State.TRUE -> style = style.withStrikethrough(true)
        TextDecoration.State.FALSE -> style = style.withStrikethrough(false)
        else -> Unit
    }
    
    when (decoration(TextDecoration.OBFUSCATED)) {
        TextDecoration.State.TRUE -> style = style.withObfuscated(true)
        TextDecoration.State.FALSE -> style = style.withObfuscated(false)
        else -> Unit
    }
    
    return style
}

internal fun MojangComponent.isEmpty(locale: Locale = Locale.US): Boolean =
    toAdventureComponent().isEmpty(locale)

/**
 * Checks whether this component is empty, meaning it has no visible characters when rendered.
 * Subject to the same limitations as [elements].
 */
fun Component.isEmpty(locale: Locale = Locale.US): Boolean = 
    elements(locale).none()

/**
 * Sets the font of this component to [font].
 * 
 * Equivalent to `font(Key.key(font))`.
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.font(font: String): B {
    return font(Key.key(font))
}

/**
 * Sets the color of this component to [color].
 * 
 * Equivalent to `color(TextColor.color(color.rgb))`.
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.color(color: Color): B {
    return color(TextColor.color(color.rgb))
}

/**
 * Appends the [fontChar's][fontChar] component to this component.
 * 
 * Equivalent to `append(fontChar.component)`.
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.append(fontChar: FontChar): B {
    return append(fontChar.component)
}

/**
 * Moves the cursor by [distance] gui-scale-affected pixels.
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.move(distance: Number): B {
    return append(MoveCharacters.getMovingComponent(distance))
}

/**
 * Moves the cursor to the beginning of the text,
 * using [lang] to localize all potential [translatable components][TranslatableComponent].
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveToStart(lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang))
}

/**
 * Moves the cursor to the center of the text,
 * using [lang] to localize all potential [translatable components][TranslatableComponent].
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveToCenter(lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang) / 2)
}

/**
 * Moves the cursor to [afterStart] pixels after the beginning of the text,
 * using [lang] to localize all potential [translatable components][TranslatableComponent].
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveTo(afterStart: Number, lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang) + afterStart.toFloat())
}

/**
 * Moves the cursor by half the width of the [component] to the left, then appends the [component].
 */
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendCentered(component: Component, lang: String = "en_us"): B {
    // -1 to account for the one unit of empty space after the last char that still counts towards the width but isn't visible
    move((CharSizes.calculateComponentWidth(component, lang) - 1) / -2f)
    return append(component)
}

/**
 * Appends a [spaces] number of space characters to this component.
 */
internal fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.indent(spaces: Int): B {
    return append(Component.text(" ".repeat(spaces)))
}

/**
 * Recursively discovers all fonts used in this component, its children,
 * and translatable arguments and returns them as a set of [Keys][Key].
 */
fun Component.getFontsRecursively(): Set<Key> {
    val defaultFont = key("minecraft", "default")
    val fonts = mutableSetOf<Key>()
    
    val queue = LinkedList<Component>()
    queue.add(this)
    
    generateSequence { queue.poll() }.forEach { current ->
        fonts += current.font() ?: defaultFont
        
        queue.addAll(current.children())
        if (current is TranslatableComponent) {
            queue.addAll(current.args())
        }
    }
    
    return fonts
}

/**
 * Converts the [Locale] into a Minecraft locale code, which is in the format of
 * `language[_country[_variant]]` (all lowercase), e.g. `en_us`.
 */
fun Locale.toMinecraftLocaleCode(): String = buildString {
    append(language.lowercase())
    if (country.isNotEmpty()) {
        append("_")
        append(country.lowercase())
        if (variant.isNotEmpty()) {
            append("_")
            append(variant.lowercase())
        }
    }
}