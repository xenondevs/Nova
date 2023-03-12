package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.commons.reflection.rawType
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.util.data.readStringLegacy
import xyz.xenondevs.nova.util.data.writeStringLegacy
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
internal object EnumBinaryAdapterLegacy : BinaryAdapterLegacy<Enum<*>> {
    
    override fun write(obj: Enum<*>, buf: ByteBuf) {
        buf.writeStringLegacy(obj.name)
    }
    
    override fun read(type: Type, buf: ByteBuf): Enum<*> {
        val clazz = type.rawType as Class<Enum<*>>
        val name = buf.readStringLegacy()
        return clazz.enumConstants.first { it.name == name }
    }
    
}