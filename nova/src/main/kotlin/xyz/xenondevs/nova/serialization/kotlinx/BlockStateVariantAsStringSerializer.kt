package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition

internal object BlockStateVariantAsStringSerializer : KSerializer<BlockStateDefinition.Variant> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.VariantAsStringSerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: BlockStateDefinition.Variant) {
        encoder.encodeString(value.properties.entries.joinToString(",") { (k, v) -> "$k=$v" })
    }
    
    override fun deserialize(decoder: Decoder): BlockStateDefinition.Variant {
        val conditions = decoder.decodeString()
            .split(",")
            .mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
        return BlockStateDefinition.Variant(conditions)
    }
    
}