package xyz.xenondevs.nova.util

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