package xyz.xenondevs.nova.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.util.readString
import xyz.xenondevs.nova.util.writeString

class NamespacedKeyElement(override val value: NamespacedKey) : BackedElement<NamespacedKey> {
    override fun getTypeId() = 24
    
    override fun write(buf: ByteBuf) {
        buf.writeString(value.namespace)
        buf.writeString(value.key)
    }
    
    override fun toString() = value.toString()
    
}

object NamespacedKeyDeserializer : BinaryDeserializer<NamespacedKeyElement> {
    override fun read(buf: ByteBuf): NamespacedKeyElement {
        @Suppress("DEPRECATION") // Other constructor doesn't accept a string namespace
        return NamespacedKeyElement(NamespacedKey(buf.readString(), buf.readString()))
    }
    
}