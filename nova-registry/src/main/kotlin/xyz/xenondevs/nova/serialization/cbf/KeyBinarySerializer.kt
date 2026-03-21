package xyz.xenondevs.nova.serialization.cbf

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

/**
 * A binary serializer for [Key] that serializes to a string of `namespace:value`.
 */
object KeyBinarySerializer : UnversionedBinarySerializer<Key>() {
    
    override fun readUnversioned(reader: ByteReader): Key {
        return Key.key(reader.readString())
    }
    
    override fun writeUnversioned(obj: Key, writer: ByteWriter) {
        writer.writeString(obj.asString())
    }
    
    override fun copyNonNull(obj: Key): Key {
        return obj
    }
    
}

/**
 * A binary serializer for [NamespacedKey] that serializes to a string of `namespace:value`.
 */
object NamespacedKeyBinarySerializer : UnversionedBinarySerializer<NamespacedKey>() {
    
    override fun readUnversioned(reader: ByteReader): NamespacedKey {
        val s = reader.readString()
        return NamespacedKey.fromString(s)
            ?: throw IllegalArgumentException("Invalid key: $s")
    }
    
    override fun writeUnversioned(obj: NamespacedKey, writer: ByteWriter) {
        writer.writeString(obj.asString())
    }
    
    override fun copyNonNull(obj: NamespacedKey): NamespacedKey {
        return obj
    }
    
}

