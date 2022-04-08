package xyz.xenondevs.nova.util.data

import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.util.ComponentUtils
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.network.chat.Component
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage
import org.bukkit.entity.Entity
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.util.localizedName
import xyz.xenondevs.nova.util.removeMinecraftFormatting
import net.minecraft.network.chat.TextComponent as NMSTextComponent

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
    return addLoreLines(*lines.map { arrayOf(TranslatableComponent(it)) }.toTypedArray())
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
    if (isEmpty()) return NMSTextComponent("")
    
    try {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to Component: ${this.contentToString()}", e)
    }
}

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

fun Array<out BaseComponent>.forceDefaultFont(): Array<out BaseComponent> {
    var previousComponent = DEFAULT_FONT_TEMPLATE
    for (component in this) {
        component.copyFormatting(previousComponent, false)
        previousComponent = component
    }
    
    return this
}

fun Array<out BaseComponent>.withoutPreFormatting(): Array<out BaseComponent> =
    ComponentUtils.withoutPreFormatting(*this)

fun BaseComponent.withoutPreFormatting(): Array<out BaseComponent> =
    ComponentUtils.withoutPreFormatting(this)
