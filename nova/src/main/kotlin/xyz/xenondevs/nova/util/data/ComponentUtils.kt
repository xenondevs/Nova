package xyz.xenondevs.nova.util.data

import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.util.ComponentUtils
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R2.util.CraftChatMessage
import org.bukkit.entity.Entity
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.util.localizedName
import xyz.xenondevs.nova.util.removeMinecraftFormatting

private val DEFAULT_FONT_TEMPLATE = ComponentBuilder("").font("default").create()[0]

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

fun localized(color: ChatColor, material: Material): BaseComponent {
    val component = material.localizedName?.let {
        TranslatableComponent(it)
    } ?: TextComponent(material.name)
    component.color = color
    return component
}

fun localized(color: ChatColor, entity: Entity): BaseComponent {
    val component = entity.localizedName?.let {
        TranslatableComponent(it)
    } ?: TextComponent(entity.name)
    component.color = color
    return component
}

fun ItemBuilder.setLocalizedName(name: String): ItemBuilder {
    return setDisplayName(TranslatableComponent(name))
}

fun ItemBuilder.setLocalizedName(chatColor: ChatColor, name: String): ItemBuilder {
    return setDisplayName(localized(chatColor, name))
}

fun ItemBuilder.addLocalizedLoreLines(vararg lines: String): ItemBuilder {
    return addLoreLines(*lines.mapToArray { arrayOf(TranslatableComponent(it)) })
}

fun ItemBuilder.addLocalizedLoreLines(chatColor: ChatColor, vararg lines: String): ItemBuilder {
    return addLoreLines(*lines.mapToArray { arrayOf(localized(chatColor, it)) })
}

fun ItemBuilder.addLoreLines(vararg lines: BaseComponent): ItemBuilder {
    return addLoreLines(*lines.map { arrayOf(it) }.toTypedArray())
}

fun Component.toBaseComponentArray(): Array<BaseComponent> {
    try {
        return ComponentSerializer.parse(CraftChatMessage.toJSON(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to BaseComponent array: $this", e)
    }
}

fun Array<out BaseComponent>.toComponent(): Component {
    if (isEmpty()) return Component.empty()
    
    try {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to Component: ${this.contentToString()}", e)
    }
}

// TODO: Option to omit text that is not in the default font
fun Array<out BaseComponent>.toPlainText(locale: String): String {
    val sb = StringBuilder()
    
    for (component in this) {
        if (component is TranslatableComponent) {
            sb.append(component.toPlainText(locale))
        } else sb.append(component.toPlainText().removeMinecraftFormatting())
    }
    
    return sb.toString()
}

fun TranslatableComponent.toPlainText(locale: String): String {
    val with = with?.map { if (it is TranslatableComponent) it.toPlainText(locale) else it.toPlainText() }
    
    val text = if (with != null)
        LocaleManager.getTranslation(locale, translate, *with.toTypedArray())
    else LocaleManager.getTranslation(locale, translate)
    
    return text.removeMinecraftFormatting()
}

fun BaseComponent.toPlainText(locale: String): String {
    return when (this) {
        is TranslatableComponent -> return toPlainText(locale)
        else -> toPlainText()
    }
}

fun Array<BaseComponent>.forceDefaultFont(): Array<BaseComponent> {
    return formatWithTemplate(DEFAULT_FONT_TEMPLATE)
}

fun Array<BaseComponent>.formatWithTemplate(template: BaseComponent): Array<BaseComponent> {
    var previousComponent = template
    for (component in this) {
        component.copyFormatting(previousComponent, false)
        previousComponent = component
    }
    
    return this
}

fun Array<BaseComponent>.withoutPreFormatting(): Array<BaseComponent> =
    ComponentUtils.withoutPreFormatting(*this)

fun BaseComponent.withoutPreFormatting(): Array<BaseComponent> =
    ComponentUtils.withoutPreFormatting(this)

fun BaseComponent.serialize(): String =
    ComponentSerializer.toString(this)

fun Array<BaseComponent>.serialize(): String =
    ComponentSerializer.toString(this)

fun BaseComponent.serializeToNBT(): StringTag =
    StringTag.valueOf(serialize())

fun Array<BaseComponent>.serializeToNBT(): StringTag =
    StringTag.valueOf(serialize())

fun ComponentBuilder.appendLocalized(translate: String, vararg with: Any): ComponentBuilder {
    append(TranslatableComponent(translate, *with))
    return this
}

fun Array<BaseComponent>.duplicate(): Array<BaseComponent> {
    return Array(size) { this[it].duplicate() }
}

object ComponentUtils {
    
    fun createLinkComponent(url: String): BaseComponent {
        return ComponentBuilder(url)
            .color(ChatColor.AQUA)
            .event(ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .create()[0]
    }
    
}
