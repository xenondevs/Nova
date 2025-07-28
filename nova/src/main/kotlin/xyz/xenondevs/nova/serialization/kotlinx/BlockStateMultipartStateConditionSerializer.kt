package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition.MultipartCase.Condition.State

internal object BlockStateMultipartStateConditionSerializer : KSerializer<State> {
    
    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor = delegate.descriptor
    
    override fun serialize(encoder: Encoder, value: State) {
        delegate.serialize(encoder, value.properties.mapValues { (_, v) -> v.joinToString("|") })
    }
    
    override fun deserialize(decoder: Decoder): State {
        val map = delegate.deserialize(decoder)
        return State(map.mapValues { (_, v) -> v.split("|").toSet() })
    }
    
}