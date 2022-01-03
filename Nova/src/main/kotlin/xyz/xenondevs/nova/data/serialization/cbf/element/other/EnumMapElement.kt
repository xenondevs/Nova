package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.Element
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import java.util.*
import kotlin.reflect.KClass

fun EnumMap<*, *>.toElement(valueClass: KClass<*>): EnumMapElement {
    val element = EnumMapElement()
    this.forEach { (constant, value) ->
        element.put(constant.name, BackedElement.createElement(if (value is Enum<*>) String::class else valueClass, value))
    }
    return element
}

class EnumMapElement : Element {
    val map = HashMap<String, BackedElement<*>>()
    
    override fun getTypeId() = 20
    
    override fun write(buf: ByteBuf) {
        buf.writeShort(map.size)
        if (map.isNotEmpty()) {
            buf.writeByte(map.values.first().getTypeId())
            map.forEach { (key, value) ->
                buf.writeString(key)
                value.write(buf)
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    inline fun <reified K : Enum<K>, V> toEnumMap(): EnumMap<K, V> {
        val enumMap = EnumMap<K, V>(K::class.java)
        map.forEach { (k, v) -> enumMap[enumValueOf(k)] = v.value as V }
        return enumMap
    }
    
    inline fun <reified K : Enum<K>, reified V : Enum<V>> toDoubleEnumMap(): EnumMap<K, V> {
        val keyEnumClass = K::class.java
        val enumMap = EnumMap<K, V>(keyEnumClass)
        map.forEach { (k, v) -> enumMap[enumValueOf(k)] = enumValueOf(v.value as String) }
        return enumMap
    }
    
    @Suppress("UNCHECKED_CAST")
    fun put(key: String, value: BackedElement<*>) {
        map[key] = value
    }
    
    override fun toString() = map.toString()
    
}

object EnumMapDeserializer : BinaryDeserializer<EnumMapElement> {
    override fun read(buf: ByteBuf): EnumMapElement {
        val size = buf.readUnsignedShort()
        if (size == 0)
            return EnumMapElement()
        
        val type = buf.readByte()
        val deserializer = BinaryDeserializer.getForType(type)
        requireNotNull(deserializer) { "Invalid type id: $type" }
        
        val map = EnumMapElement()
        repeat(size) {
            val key = buf.readString()
            val value = deserializer.read(buf) as BackedElement<*>
            map.put(key, value)
        }
        
        return map
    }
    
}