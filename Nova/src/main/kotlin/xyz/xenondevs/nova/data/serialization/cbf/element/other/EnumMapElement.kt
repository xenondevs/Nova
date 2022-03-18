package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import org.checkerframework.checker.units.qual.K
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.DeserializerRegistry
import xyz.xenondevs.nova.data.serialization.cbf.Element
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import xyz.xenondevs.nova.util.reflection.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

// TODO: clean up
class EnumMapElement : Element {
    
    val map = HashMap<String, Element>()
    
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
    
    fun put(key: String, value: Element) {
        map[key] = value
    }
    
    @Suppress("UNCHECKED_CAST")
    inline fun <reified K : Enum<K>, reified V> toEnumMap(): EnumMap<K, V> {
        return toEnumMap(type<K>(), type<V>()) as EnumMap<K, V>
    }
    
    @Suppress("UNCHECKED_CAST")
    fun toEnumMap(enumType: Type, valueType: Type): EnumMap<*, *> {
        val enumClass = enumType.representedClass as Class<Enum<*>>
        val enumMap = ReflectionRegistry.ENUM_MAP_CONSTRUCTOR.newInstance(enumClass) as MutableMap<Any, Any>
        map.forEach { (name, element) ->
            val enum = enumValueOf(enumClass, name)
            val value = getValue(element, valueType)
            
            enumMap[enum] = value
        }
        
        return enumMap as EnumMap<*, *>
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun getValue(element: Element, type: Type): Any {
        val typeClass = type.representedClass
        return when {
            typeClass != null && typeClass.isEnum -> enumValueOf(typeClass as Class<Enum<*>>, (element as BackedElement<String>).value)
            element is EnumMapElement -> {
                val parameterizedType = type.tryTakeUpperBound() as ParameterizedType
                return element.toEnumMap(parameterizedType.actualTypeArguments[0], parameterizedType.actualTypeArguments[1])
            }
            else -> (element as BackedElement<Any>).value
        }
    }
    
    override fun toString() = map.toString()
    
    companion object {
        
        fun of(enumMap: Map<*, *>, valueType: Type): EnumMapElement {
            val enumMapElement = EnumMapElement()
            enumMap.forEach { (key, value) ->
                key as Enum<*>
                value as Any
                
                val valueElement = when {
                    value is Enum<*> -> BackedElement.createElement(value.name)
                    value is EnumMap<*, *> -> {
                        val parameterizedType = valueType.tryTakeUpperBound() as ParameterizedType
                        of(value, parameterizedType.actualTypeArguments[1])
                    }
                    value is Map<*, *> && (valueType.tryTakeUpperBound() as ParameterizedType).actualTypeArguments[0].representedClass?.isEnum ?: false -> {
                        val parameterizedType = valueType.tryTakeUpperBound() as ParameterizedType
                        of(value, parameterizedType.actualTypeArguments[1])
                    }
                    else -> BackedElement.createElement(value)
                }
                enumMapElement.put(key.name, valueElement)
            }
            return enumMapElement
        }
        
    }
    
}

object EnumMapDeserializer : BinaryDeserializer<EnumMapElement> {
    override fun read(buf: ByteBuf): EnumMapElement {
        val size = buf.readUnsignedShort()
        if (size == 0)
            return EnumMapElement()
        
        val type = buf.readByte()
        val deserializer = DeserializerRegistry.getForType(type)
        requireNotNull(deserializer) { "Invalid type id: $type" }
        
        val map = EnumMapElement()
        repeat(size) {
            val key = buf.readString()
            val value = deserializer.read(buf)
            map.put(key, value)
        }
        
        return map
    }
    
}