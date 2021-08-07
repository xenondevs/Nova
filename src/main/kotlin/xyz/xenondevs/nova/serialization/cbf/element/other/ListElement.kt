package xyz.xenondevs.nova.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element
import xyz.xenondevs.nova.util.writeByte

/**
 * Currently only supports 1 type (WITHOUT CHECKS!)
 */
class ListElement<T : Element> : Element {
    
    val list = ArrayList<T>()
    
    override fun getTypeId() = 19
    
    override fun write(buf: ByteBuf) {
        require(list.size <= 65535) { "list is too large!" }
        buf.writeShort(list.size)
        if (list.isNotEmpty()) {
            buf.writeByte(list.first().getTypeId())
            list.forEach { it.write(buf) }
        }
    }
    
    fun add(element: T) {
        list.add(element)
    }
    
    fun remove(element: T) {
        list.remove(element)
    }
    
    @Suppress("UNCHECKED_CAST") // Not the lists responsibility
    inline fun <reified V, C : MutableCollection<in V>> toCollection(dest: C): C {
        list.map { (it as BackedElement<V>).value }.toCollection(dest)
        return dest
    }
    
    @Suppress("UNCHECKED_CAST") // Not the lists responsibility
    inline fun <reified E : Enum<E>, C : MutableCollection<in E>> toEnumCollection(dest: C): C {
        list.map { enumValueOf<E>((it as BackedElement<String>).value) }.toCollection(dest)
        return dest
    }
    
    override fun toString() = list.toString()
    
}

object ListDeserializer : BinaryDeserializer<ListElement<*>> {
    override fun read(buf: ByteBuf): ListElement<*> {
        val elements = ListElement<Element>()
        val size = buf.readUnsignedShort()
        if (size > 0) {
            val type = buf.readByte()
            val deserializer = BinaryDeserializer.getForType(type)
            requireNotNull(deserializer) { "Illegal type: $type" }
            repeat(size) {
                elements.add(deserializer.read(buf))
            }
        }
        return elements
    }
}