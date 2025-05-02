package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

internal object BackingStateConfigSerializer : KSerializer<BackingStateConfig> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.BackingStateConfig") {
        element<String>("type")
        element<Int>("id")
        element<Boolean>("waterlogged")
    }
    
    override fun serialize(encoder: Encoder, value: BackingStateConfig) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, String.serializer(), value.type::class.jvmName)
            encodeSerializableElement(descriptor, 1, Int.serializer(), value.id)
            encodeSerializableElement(descriptor, 2, Boolean.serializer(), value.waterlogged)
        }
    }
    
    override fun deserialize(decoder: Decoder): BackingStateConfig {
        return decoder.decodeStructure(descriptor) {
            var typeName: String? = null
            var id: Int? = null
            var waterlogged: Boolean? = null
            
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> typeName = decodeStringElement(descriptor, index)
                    1 -> id = decodeIntElement(descriptor, index)
                    2 -> waterlogged = decodeBooleanElement(descriptor, index)
                    else -> break
                }
            }
            
            requireNotNull(typeName)
            requireNotNull(id)
            requireNotNull(waterlogged)
            
            val kclass = Class.forName(typeName).kotlin
            val type = (kclass.objectInstance ?: kclass.companionObjectInstance) as BackingStateConfigType<*>
            
            return@decodeStructure type.of(id, waterlogged)
        }
    }
    
}