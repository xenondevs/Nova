package xyz.xenondevs.nmsutils.internal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.craftbukkit.v1_19_R3.util.CraftChatMessage
import net.minecraft.network.chat.Component as MojangComponent

internal fun MojangComponent.toBaseComponentArray(): Array<out BaseComponent> {
    try {
        return ComponentSerializer.parse(CraftChatMessage.toJSON(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to BaseComponent array: $this", e)
    }
}

internal fun MojangComponent.toAdventureComponent(): Component {
    return GsonComponentSerializer.gson().deserialize(CraftChatMessage.toJSON(this))
}

internal fun MojangComponent.toJson(): String {
    return MojangComponent.Serializer.toJson(this)
}

internal fun Component.toNmsComponent(): MojangComponent {
    return CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(this))
}

internal fun Component.toJson(): String {
    return GsonComponentSerializer.gson().serialize(this)
}

internal fun Array<out BaseComponent>.toNmsComponent(): MojangComponent {
    if (isEmpty()) return MojangComponent.empty()
    
    try {
        return CraftChatMessage.fromJSON(toJson())
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to Component: ${this.contentToString()}", e)
    }
}

internal fun Array<out BaseComponent>.toJson(): String {
    return ComponentSerializer.toString(this)
}

internal fun String.toNmsComponent(): MojangComponent {
    return MojangComponent.Serializer.fromJson(this)!!
}

internal fun String.toBaseComponentArray(): Array<out BaseComponent> {
    return ComponentSerializer.parse(this)
}

internal fun String.toAdventureComponent(): Component {
    return GsonComponentSerializer.gson().deserialize(this)
}
