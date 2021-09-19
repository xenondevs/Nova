package xyz.xenondevs.nova.data.serialization.cbf.element

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.Element
import xyz.xenondevs.nova.data.serialization.cbf.element.other.NullElement
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.toByteArray
import xyz.xenondevs.nova.util.data.writeString

class CompoundElement : BackedElement<CompoundElement>() {
    
    override val value = this
    private val elements = HashMap<String, Element>()
    
    override fun getTypeId() = 18
    
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
    
    inline fun <reified T : Any> put(key: String, value: T?) {
        when (value) {
            null -> putElement(key, NullElement)
            is Element -> putElement(key, value)
            else -> putElement(key, BackedElement.createElement(value))
        }
    }
    
    fun remove(key: String) {
        elements.remove(key)
    }
    
    operator fun contains(key: String) = key in elements
    
    @Suppress("UNCHECKED_CAST") // Not the compounds responsibility
    fun <T : Element> getElement(key: String) = elements[key] as T?
    
    fun <T : Element> getAssertedElement(key: String) = getElement<T>(key)!!
    
    @Suppress("UNCHECKED_CAST") // Not the compounds responsibility
    inline fun <reified T> get(key: String): T? {
        if (!contains(key))
            return null
        return getElement<BackedElement<T>>(key)!!.value
    }
    
    @Suppress("UNCHECKED_CAST") // Not the compounds responsibility
    inline fun <reified T> getAsserted(key: String): T {
        return getElement<BackedElement<T>>(key)!!.value
    }
    
    inline fun <reified T : Enum<T>> getEnumConstant(key: String): T? {
        val constant: String = get(key) ?: return null
        return enumValueOf<T>(constant)
    }
    
    override fun toString(): String {
        return toString(depth = 0)
    }
    
    fun toString(depth: Int): String {
        val depthPrefix = " ".repeat(depth)
        val builder = StringBuilder("{\n")
        elements.forEach { (key, value) ->
            val valueString = if (value is CompoundElement) value.toString(depth + 1) else value.toString()
            builder.append("$depthPrefix\"$key\": $valueString\n")
        }
        builder.append("$depthPrefix}")
        return builder.toString()
    }
    
    fun toByteArray() = Unpooled.buffer().also(this::write).toByteArray()
    
    fun isEmpty() = elements.isEmpty()
    
    fun isNotEmpty() = elements.isNotEmpty()
    
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