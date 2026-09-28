package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [ItemDamageFunction] from an object with optional fields `threshold`, `base`, `factor` (all floats).
 */
internal object ItemDamageFunctionKSerializer : KSerializer<ItemDamageFunction> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.ItemDamageFunction") {
        element<Float>("threshold", isOptional = true)
        element<Float>("base", isOptional = true)
        element<Float>("factor", isOptional = true)
    }
    
    override fun deserialize(decoder: Decoder): ItemDamageFunction {
        val composite = decoder.beginStructure(descriptor)
        var threshold: Float? = null
        var base: Float? = null
        var factor: Float? = null
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> threshold = composite.decodeFloatElement(descriptor, 0)
                1 -> base = composite.decodeFloatElement(descriptor, 1)
                2 -> factor = composite.decodeFloatElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }
        composite.endStructure(descriptor)
        
        val builder = ItemDamageFunction.itemDamageFunction()
        if (threshold != null) builder.threshold(threshold)
        if (base != null) builder.base(base)
        if (factor != null) builder.factor(factor)
        return builder.build()
    }
    
    override fun serialize(encoder: Encoder, value: ItemDamageFunction) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeFloatElement(descriptor, 0, value.threshold())
        composite.encodeFloatElement(descriptor, 1, value.base())
        composite.encodeFloatElement(descriptor, 2, value.factor())
        composite.endStructure(descriptor)
    }
    
}
