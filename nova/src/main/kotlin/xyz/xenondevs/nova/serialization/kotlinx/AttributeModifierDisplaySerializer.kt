package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Serializes [AttributeModifierDisplay] as a string: `"default"` for [AttributeModifierDisplay.Default],
 * `"hidden"` for [AttributeModifierDisplay.Hidden], or a MiniMessage string for [AttributeModifierDisplay.OverrideText].
 */
internal object AttributeModifierDisplaySerializer : KSerializer<AttributeModifierDisplay> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.AttributeModifierDisplay", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder): AttributeModifierDisplay {
        return when (val str = decoder.decodeString()) {
            "default" -> AttributeModifierDisplay.reset()
            "hidden" -> AttributeModifierDisplay.hidden()
            else -> AttributeModifierDisplay.override(MiniMessage.miniMessage().deserialize(str))
        }
    }
    
    override fun serialize(encoder: Encoder, value: AttributeModifierDisplay) {
        val str = when (value) {
            is AttributeModifierDisplay.Default -> "default"
            is AttributeModifierDisplay.Hidden -> "hidden"
            is AttributeModifierDisplay.OverrideText -> MiniMessage.miniMessage().serialize(value.text())
            else -> throw SerializationException("Unknown AttributeModifierDisplay type: ${value::class}")
        }
        encoder.encodeString(str)
    }
    
}
