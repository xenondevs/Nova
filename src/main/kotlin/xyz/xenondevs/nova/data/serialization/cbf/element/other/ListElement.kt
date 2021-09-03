package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.Element

@Suppress("UNCHECKED_CAST")
class ListElement : Element {
    
    val list = ArrayList<Element>()
    
    override fun getTypeId() = 19
    
    override fun write(buf: ByteBuf) {
        require(list.size <= 65535) { "list is too large!" }
        
        list.forEach { element ->
            buf.writeByte(element.getTypeId())
            element.write(buf)
        }
        buf.writeByte(0)
    }
    
    fun addElement(value: Element) {
        list += value
    }
    
    inline fun <reified T : Any> add(value: T?) {
        when (value) {
            null -> addElement(NullElement)
            is Element -> addElement(value)
            else -> addElement(BackedElement.createElement(value))
        }
    }
    
    inline operator fun <reified T : Any> plusAssign(value: T?) = add(value)
    
    fun removeElement(value: Element) {
        list -= value
    }
    
    inline fun <reified T : Any> remove(value: T?) {
        when (value) {
            null -> removeElement(NullElement)
            is Element -> removeElement(value)
            else -> removeElement(BackedElement.createElement(value))
        }
    }
    
    inline operator fun <reified T : Any> minusAssign(value: T?) = remove(value)
    
    inline operator fun <reified V : Any> get(index: Int): V {
        return (list[index] as BackedElement<V>).value
    }
    
    inline fun <reified V, C : MutableCollection<in V>> toCollection(dest: C): C {
        list.map { (it as BackedElement<V>).value }.toCollection(dest)
        return dest
    }
    
    inline fun <reified V> toTypedArray(): Array<V> {
        return list.map { (it as BackedElement<V>).value }.toTypedArray()
    }
    
    inline fun <reified E : Enum<E>, C : MutableCollection<in E>> toEnumCollection(dest: C): C {
        list.map { enumValueOf<E>((it as BackedElement<String>).value) }.toCollection(dest)
        return dest
    }
    
    override fun toString() = list.toString()
    
}

object ListDeserializer : BinaryDeserializer<ListElement> {
    override fun read(buf: ByteBuf): ListElement {
        val elements = ListElement()
        var currentType: Byte
        while ((buf.readByte().also { currentType = it }) != 0.toByte()) {
            val deserializer = BinaryDeserializer.getForType(currentType)
            requireNotNull(deserializer) { "Invalid type id: $currentType" }
            elements.add(deserializer.read(buf))
        }
        return elements
    }
}