package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.DyeColor

// https://github.com/Kotlin/kotlinx.serialization/issues/2762
internal abstract class LowercaseEnumSerializer<E : Enum<E>>(enumClass: Class<E>) : KSerializer<E> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.LowercaseEnumSerializer", PrimitiveKind.STRING)
    
    private val lookup = enumClass.enumConstants.associateByTo(HashMap()) { it.name.lowercase() }
    
    override fun deserialize(decoder: Decoder): E {
        val str = decoder.decodeString()
        return lookup[str] ?: throw IllegalArgumentException("Unknown lowercase enum value: $str")
    }
    
    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(value.name.lowercase())
    }
    
}

internal object LowercaseDyeColorSerializer : LowercaseEnumSerializer<DyeColor>(DyeColor::class.java)