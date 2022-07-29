package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.NamespacedId
import java.lang.reflect.Type

internal object NamespacedKeyBinaryAdapter : BinaryAdapter<NamespacedKey> {
    
    override fun read(type: Type, buf: ByteBuffer): NamespacedKey {
        return NamespacedKey.fromString(buf.readString())!!
    }
    
    override fun write(obj: NamespacedKey, buf: ByteBuffer) {
        buf.writeString(obj.toString())
    }
    
}

internal object NamespacedIdBinaryAdapter : BinaryAdapter<NamespacedId> {
    
    override fun read(type: Type, buf: ByteBuffer): NamespacedId {
        return NamespacedId.of(buf.readString())
    }
    
    override fun write(obj: NamespacedId, buf: ByteBuffer) {
        buf.writeString(obj.toString())
    }
    
}