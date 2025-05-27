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
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import kotlin.jvm.optionals.getOrNull

@Suppress("UNCHECKED_CAST")
internal object BlockStateSerializer : KSerializer<BlockState> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.BlockState") {
        element("block", BlockSerializer.descriptor)
        element<Map<String, String>>("properties")
    }
    
    override fun serialize(encoder: Encoder, value: BlockState) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, BlockSerializer, value.block)
            
            val map = value.values as Map<Property<Any>, Comparable<Any>>
            encodeSerializableElement(
                descriptor, 1,
                MapSerializer(String.serializer(), String.serializer()),
                map.entries.associate { (property, value) -> property.name to property.getName(value as Any) }
            )
        }
    }
    
    override fun deserialize(decoder: Decoder): BlockState {
        return decoder.decodeStructure(descriptor) {
            var block: Block? = null
            var properties: Map<String, String> = emptyMap()
            
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> block = decodeSerializableElement(descriptor, index, BlockSerializer)
                    1 -> properties = decodeSerializableElement(
                        descriptor, index,
                        MapSerializer(String.serializer(), String.serializer())
                    )
                    else -> break
                }
            }
            
            if (block == null)
                throw SerializationException("Missing property 'block'")
            
            val allPropertiesByName = block.defaultBlockState().properties.associateBy { it.name }
            return@decodeStructure properties.entries.fold(block.defaultBlockState()) { blockState, (name, value) ->
                blockState.setValue<String, String>(allPropertiesByName, name, value)
            }
        }
    }
    
    private fun <T : Comparable<T>, V : T> BlockState.setValue(
        properties: Map<String, Property<*>>,
        propertyName: String,
        valueName: String
    ): BlockState {
        val property = properties[propertyName] as? Property<T>
            ?: throw SerializationException("$this does not have property named $propertyName")
        val value = property.getValue(valueName).getOrNull() as? V
            ?: throw SerializationException("BlockState $this property $propertyName does not have value named $valueName")
        
        return setValue(property, value)
    }
    
}