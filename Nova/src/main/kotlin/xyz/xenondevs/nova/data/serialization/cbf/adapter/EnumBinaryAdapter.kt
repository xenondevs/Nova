package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import xyz.xenondevs.nova.util.reflection.representedClass
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
internal object EnumBinaryAdapter : BinaryAdapter<Enum<*>> {
    
    override fun write(obj: Enum<*>, buf: ByteBuf) {
        buf.writeString(obj.name)
    }
    
    override fun read(type: Type, buf: ByteBuf): Enum<*> {
        val clazz = type.representedClass as Class<Enum<*>>
        val name = buf.readString()
        return clazz.enumConstants.first { it.name == name }
    }
    
}