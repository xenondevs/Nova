package xyz.xenondevs.nova.data.serialization.cbf

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.EmptyDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.other.*
import xyz.xenondevs.nova.data.serialization.cbf.element.primitive.*
import xyz.xenondevs.nova.util.data.toByteBuf

interface BinaryDeserializer<T : Element> {
    
    fun read(bytes: ByteArray) = read(bytes.toByteBuf())
    
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
            BooleanArrayDeserializer, // 9
            ByteArrayDeserializer, // 10
            IntArrayDeserializer, // 11
            CharArrayDeserializer, // 12
            FloatArrayDeserializer, // 13
            LongArrayDeserializer, // 14
            DoubleArrayDeserializer, // 15
            StringArrayDeserializer, // 16
            NullDeserializer, // 17
            CompoundDeserializer, // 18
            ListDeserializer, // 19
            EnumMapDeserializer, // 20
            UUIDDeserializer, // 21
            ItemStackDeserializer, // 22
            LocationDeserializer, // 23
            NamespacedKeyDeserializer, // 24
            UpgradesDeserializer, // 25
            VirtualInventoryDeserializer, // 26
            ColorDeserializer // 27
        )
        
        fun getForType(type: Byte) = DESERIALIZERS.getOrNull(type.toInt())
        
    }
    
}