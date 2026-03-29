package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Serializable type alias for [Component] using [ComponentAsMiniMessageSerializer].
 */
typealias ComponentAsMiniMessage = @Serializable(with = ComponentAsMiniMessageSerializer::class) Component

/**
 * Serializes [Component] as a MiniMessage-formatted string.
 * Legacy section signs (`§`) are stripped during deserialization.
 */
object ComponentAsMiniMessageSerializer : KSerializer<Component> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.Component", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder): Component {
        val message = decoder.decodeString().replace("§", "")
        return MiniMessage.miniMessage().deserialize(message)
    }
    
    override fun serialize(encoder: Encoder, value: Component) {
        encoder.encodeString(MiniMessage.miniMessage().serialize(value))
    }
    
}
