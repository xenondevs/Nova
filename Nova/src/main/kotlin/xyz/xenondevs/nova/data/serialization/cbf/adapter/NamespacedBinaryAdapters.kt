package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.util.data.readStringLegacy
import xyz.xenondevs.nova.util.data.writeStringLegacy
import java.lang.reflect.Type

internal object NamespacedKeyBinaryAdapter : BinaryAdapter<NamespacedKey> {
    
    override fun write(obj: NamespacedKey, buf: ByteBuf) {
        buf.writeStringLegacy(obj.toString())
    }
    
    override fun read(type: Type, buf: ByteBuf): NamespacedKey {
        return NamespacedKey.fromString(buf.readStringLegacy())!!
    }
    
}

internal object NamespacedIdBinaryAdapter : BinaryAdapter<NamespacedId> {
    
    override fun write(obj: NamespacedId, buf: ByteBuf) {
        buf.writeStringLegacy(obj.toString())
    }
    
    override fun read(type: Type, buf: ByteBuf): NamespacedId {
        return NamespacedId.of(buf.readStringLegacy())
    }
    
}