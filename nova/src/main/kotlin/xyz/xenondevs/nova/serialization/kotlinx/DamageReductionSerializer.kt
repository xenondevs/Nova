package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction
import io.papermc.paper.registry.set.RegistryKeySet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.damage.DamageType

/**
 * Serializes [DamageReduction] from an object with optional fields `base`, `factor`,
 * `horizontal_blocking_angle` (all floats), and `type` (a damage type tag/key set).
 */
internal object DamageReductionSerializer : KSerializer<DamageReduction> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.DamageReduction") {
        element<Float>("base", isOptional = true)
        element<Float>("factor", isOptional = true)
        element<Float>("horizontal_blocking_angle", isOptional = true)
        element("type", DamageTypeKeySetSerializer.descriptor, isOptional = true)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): DamageReduction {
        val composite = decoder.beginStructure(descriptor)
        var base: Float? = null
        var factor: Float? = null
        var horizontalBlockingAngle: Float? = null
        var type: RegistryKeySet<DamageType>? = null
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> base = composite.decodeFloatElement(descriptor, 0)
                1 -> factor = composite.decodeFloatElement(descriptor, 1)
                2 -> horizontalBlockingAngle = composite.decodeFloatElement(descriptor, 2)
                3 -> type = composite.decodeSerializableElement(descriptor, 3, DamageTypeKeySetSerializer)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }
        composite.endStructure(descriptor)
        
        val builder = DamageReduction.damageReduction()
        if (base != null) builder.base(base)
        if (factor != null) builder.factor(factor)
        if (horizontalBlockingAngle != null) builder.horizontalBlockingAngle(horizontalBlockingAngle)
        if (type != null) builder.type(type)
        return builder.build()
    }
    
    override fun serialize(encoder: Encoder, value: DamageReduction) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeFloatElement(descriptor, 0, value.base())
        composite.encodeFloatElement(descriptor, 1, value.factor())
        composite.encodeFloatElement(descriptor, 2, value.horizontalBlockingAngle())
        val type = value.type()
        if (type != null) composite.encodeSerializableElement(descriptor, 3, DamageTypeKeySetSerializer, type)
        composite.endStructure(descriptor)
    }
    
}
