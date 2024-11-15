@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.util.component.adventure

import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import org.bukkit.entity.Player
import xyz.xenondevs.inventoryaccess.util.AdventureComponentUtils
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.ui.overlay.MoveCharacters
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.toResourceLocation
import java.awt.Color
import java.util.LinkedList
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
    return MojangComponent.Serializer.toJson(this, REGISTRY_ACCESS)
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
    return AdventureComponentUtils.withoutPreFormatting(this)
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
    font()?.let { style = style.withFont(it.toResourceLocation()) }
    
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

internal fun MojangComponent.isEmpty(): Boolean {
    val queue = LinkedList<MojangComponent>()
    queue.add(this)
    
    while (queue.isNotEmpty()) {
        val current = queue.poll()
        
        when (val contents = current.contents) {
            is PlainTextContents -> {
                if (contents.text().isNotEmpty())
                    return false
            }
            
            is TranslatableContents -> {
                if (contents.key.isNotEmpty() || !contents.fallback.isNullOrEmpty())
                    return false
                
                for (arg in contents.args) {
                    if (arg is MojangComponent) {
                        queue.add(arg)
                    }
                }
            }
            
            else -> return false // TODO: support other content types
        }
        
        queue.addAll(current.siblings)
    }
    
    return true
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.font(font: String): B {
    return font(Key.key(font))
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.color(color: Color): B {
    return color(TextColor.color(color.rgb))
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.append(fontChar: FontChar): B {
    return append(fontChar.component)
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.move(distance: Number): B {
    return append(MoveCharacters.getMovingComponent(distance))
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveToStart(lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang))
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveToCenter(lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang) / 2)
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.moveTo(afterStart: Number, lang: String = "en_us"): B {
    return move(-CharSizes.calculateComponentWidth(build(), lang) + afterStart.toFloat())
}

internal fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.indent(spaces: Int): B {
    return append(Component.text(" ".repeat(spaces)))
} 