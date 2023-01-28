package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.data.NamespacedId
import java.lang.reflect.Type

internal object NamespacedKeyBinaryAdapter : BinaryAdapter<NamespacedKey> {
    
    override fun read(type: Type, reader: ByteReader): NamespacedKey {
        return NamespacedKey.fromString(reader.readString())!!
    }
    
    override fun write(obj: NamespacedKey, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}

internal object NamespacedIdBinaryAdapter : BinaryAdapter<NamespacedId> {
    
    override fun read(type: Type, reader: ByteReader): NamespacedId {
        return NamespacedId.of(reader.readString())
    }
    
    override fun write(obj: NamespacedId, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}