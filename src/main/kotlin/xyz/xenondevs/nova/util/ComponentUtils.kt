package xyz.xenondevs.nova.util

import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent

fun coloredText(color: ChatColor, text: Any): TextComponent {
    val component = TextComponent(text.toString())
    component.color = color
    return component
}

fun localized(color: ChatColor, translate: String, vararg with: Any): TranslatableComponent {
    val component = TranslatableComponent(translate, *with)
    component.color = color
    return component
}

fun <T> T.clean(): T where T : BaseComponent {
    if (colorRaw == null) color = ChatColor.WHITE
    if (isItalicRaw == null) isItalic = false
    
    return this
}

fun ItemBuilder.setLocalizedName(name: String): ItemBuilder {
    return setDisplayName(TranslatableComponent(name))
}

fun ItemBuilder.addLocalizedLoreLines(vararg lines: String): ItemBuilder {
    return addLoreLines(*lines.map { arrayOf(TranslatableComponent(it)) }.toTypedArray())
}

fun ItemBuilder.addLoreLines(vararg lines: BaseComponent): ItemBuilder {
    return addLoreLines(*lines.map { arrayOf(it) }.toTypedArray())
}
