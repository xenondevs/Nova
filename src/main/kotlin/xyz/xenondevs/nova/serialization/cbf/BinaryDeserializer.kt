package xyz.xenondevs.nova.serialization.cbf

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.EmptyDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.other.ItemStackDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.other.UUIDDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.primitive.*

interface BinaryDeserializer<T : Element> {
    
    fun read(buf: ByteBuf): T
    
    companion object DeserializerRegistry {
        private val DESERIALIZERS = arrayOf(
            EmptyDeserializer, // 0
            BooleanDeserializer, // 1
            ByteDeserializer, // 2
            IntDeserializer, // 3
            CharDeserializer, // 4
            FloatDeserializer, // 5
            LongDeserializer, // 6
            DoubleDeserializer, // 7
            StringDeserializer, // 8
            CompoundDeserializer, // 9
            BooleanArrayDeserializer, // 10
            ByteArrayDeserializer, // 11
            IntArrayDeserializer, // 12
            CharArrayDeserializer, // 13
            FloatArrayDeserializer, // 14
            LongArrayDeserializer, // 15
            DoubleArrayDeserializer, // 16
            StringArrayDeserializer, // 17
            UUIDDeserializer, // 18
            ItemStackDeserializer, // 19
        )
        
        fun getForType(type: Byte) = runCatching { DESERIALIZERS[type.toInt()] }.getOrNull()
    }
    
}