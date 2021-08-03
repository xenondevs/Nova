package xyz.xenondevs.nova.serialization.cbf.element

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element
import xyz.xenondevs.nova.util.readString
import xyz.xenondevs.nova.util.writeByte
import xyz.xenondevs.nova.util.writeString

class CompoundElement : Element {
    
    private val elements = HashMap<String, Element>()
    
    override fun getTypeId() = 9.toByte()
    
    override fun write(buf: ByteBuf) {
        elements.forEach { (key, element) ->
            buf.writeByte(element.getTypeId())
            buf.writeString(key)
            element.write(buf)
        }
        buf.writeByte(0)
    }
    
    fun putElement(key: String, value: Element) {
        elements[key] = value
    }
    
    inline fun <reified T : Any> put(key: String, value: T) {
        putElement(key, BackedElement.createElement(value))
    }
    
    fun getElement(key: String) = elements[key]
    
    @Suppress("UNCHECKED_CAST") // Not the compounds responsibility
    inline fun <reified T> get(key: String): T {
        return (getElement(key) as BackedElement<T>).value
    }
    
    inline fun <reified T : Enum<T>> getEnum(key: String) = enumValueOf<T>(key)
    
    override fun toString(): String {
        val builder = StringBuilder("{\n")
        elements.forEach { (key, value) ->
            builder.append("\"$key\": $value\n")
        }
        builder.append("}")
        return builder.toString()
    }
    
}

object CompoundDeserializer : BinaryDeserializer<CompoundElement> {
    
    override fun read(buf: ByteBuf): CompoundElement {
        val compound = CompoundElement()
        var currentType: Byte
        while ((buf.readByte().also { currentType = it }) != 0.toByte()) {
            val deserializer = BinaryDeserializer.getForType(currentType)
            requireNotNull(deserializer) { "Invalid type id: $currentType" }
            compound.putElement(buf.readString(), deserializer.read(buf))
        }
        return compound
    }
    
}