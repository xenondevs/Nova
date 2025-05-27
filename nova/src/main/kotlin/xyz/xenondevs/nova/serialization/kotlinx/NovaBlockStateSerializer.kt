package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

@Suppress("UNCHECKED_CAST")
internal object NovaBlockStateSerializer : KSerializer<NovaBlockState> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.NovaBlockState") {
        element<NovaBlock>("block")
        element<Map<String, String>>("properties")
    }
    
    override fun serialize(encoder: Encoder, value: NovaBlockState) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, NovaBlock.serializer(), value.block)
            
            val map = value.scopedValues as Map<ScopedBlockStateProperty<Any>, Any>
            encodeSerializableElement(
                descriptor, 1,
                MapSerializer(String.serializer(), String.serializer()),
                map.entries.associate { (property, value) -> property.property.id.toString() to property.valueToString(value) }
            )
        }
    }
    
    override fun deserialize(decoder: Decoder): NovaBlockState {
        return decoder.decodeStructure(descriptor) {
            var block: NovaBlock? = null
            var properties: Map<String, String> = emptyMap()
            
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> block = decodeSerializableElement(descriptor, index, NovaBlock.serializer())
                    1 -> properties = decodeSerializableElement(
                        descriptor, index,
                        MapSerializer(String.serializer(), String.serializer())
                    )
                    else -> break
                }
            }
            
            if (block == null)
                throw SerializationException("Missing property 'block'")
            
            var blockState = block.defaultBlockState
            for ((propertyId, propertyValueStr) in properties) {
                val property = block.stateProperties.firstOrNull { it.property.id.toString() == propertyId } as ScopedBlockStateProperty<Any>?
                    ?: throw SerializationException("Unknown property '$propertyId'")
                val propertyValue = property.stringToValue(propertyValueStr)
                
                blockState = blockState.with(property.property, propertyValue)
            }
            
            return@decodeStructure blockState
        }
    }
    
}