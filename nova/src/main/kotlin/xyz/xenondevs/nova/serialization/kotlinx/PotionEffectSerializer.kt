package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Serializes [PotionEffect] as an object with fields `type` (namespaced key resolved via the
 * [PotionEffectType] contextual serializer), `duration`, `amplifier`, and optional `ambient`,
 * `particles`, `icon` (all defaulting to `true`).
 */
internal object PotionEffectSerializer : KSerializer<PotionEffect> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.PotionEffect") {
        element("type", PotionEffectTypeSerializer.descriptor)
        element<Int>("duration")
        element<Int>("amplifier")
        element("ambient", Boolean.serializer().descriptor, isOptional = true)
        element("particles", Boolean.serializer().descriptor, isOptional = true)
        element("icon", Boolean.serializer().descriptor, isOptional = true)
    }
    
    override fun deserialize(decoder: Decoder): PotionEffect {
        val composite = decoder.beginStructure(descriptor)
        var type: PotionEffectType? = null
        var duration: Int? = null
        var amplifier: Int? = null
        var ambient = true
        var particles = true
        var icon = true
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> type = composite.decodeSerializableElement(descriptor, 0, PotionEffectTypeSerializer)
                1 -> duration = composite.decodeIntElement(descriptor, 1)
                2 -> amplifier = composite.decodeIntElement(descriptor, 2)
                3 -> ambient = composite.decodeBooleanElement(descriptor, 3)
                4 -> particles = composite.decodeBooleanElement(descriptor, 4)
                5 -> icon = composite.decodeBooleanElement(descriptor, 5)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }
        composite.endStructure(descriptor)
        
        return PotionEffect(
            type ?: throw SerializationException("Missing 'type'"),
            duration ?: throw SerializationException("Missing 'duration'"),
            amplifier ?: throw SerializationException("Missing 'amplifier'"),
            ambient, particles, icon
        )
    }
    
    override fun serialize(encoder: Encoder, value: PotionEffect) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, PotionEffectTypeSerializer, value.type)
        composite.encodeIntElement(descriptor, 1, value.duration)
        composite.encodeIntElement(descriptor, 2, value.amplifier)
        composite.encodeBooleanElement(descriptor, 3, value.isAmbient)
        composite.encodeBooleanElement(descriptor, 4, value.hasParticles())
        composite.encodeBooleanElement(descriptor, 5, value.hasIcon())
        composite.endStructure(descriptor)
    }
    
}
