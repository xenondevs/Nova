package xyz.xenondevs.nova.util.component.adventure

import io.papermc.paper.adventure.AdventureComponent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.nbt.StringTag
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage
import org.bukkit.entity.Player
import xyz.xenondevs.inventoryaccess.util.AdventureComponentUtils
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import java.awt.Color
import net.minecraft.network.chat.Component as MojangComponent

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
    if (this is AdventureComponent)
        return this.`adventure$component`()
    
    return GsonComponentSerializer.gson().deserialize(CraftChatMessage.toJSON(this))
}

fun MojangComponent.toJson(): String {
    return MojangComponent.Serializer.toJson(this)
}

fun Component.toNMSComponent(): MojangComponent {
    return AdventureComponent(this)
}

fun Component.toJson(): String {
    return GsonComponentSerializer.gson().serialize(this)
}

fun Component.toNBT(): StringTag {
    return StringTag.valueOf(toJson())
}

@Suppress("DEPRECATION")
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
