package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

/**
 * A binary serializer for [TagKey] that serializes to a string of `namespace:value`.
 */
class TagKeyBinarySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>
) : UnversionedBinarySerializer<TagKey<T>>() {
    
    override fun readUnversioned(reader: ByteReader): TagKey<T> {
        val key = Key.key(reader.readString())
        return TagKey.create(registryKey, key)
    }
    
    override fun writeUnversioned(obj: TagKey<T>, writer: ByteWriter) {
        writer.writeString(obj.key().asString())
    }
    
    override fun copyNonNull(obj: TagKey<T>): TagKey<T> {
        return obj
    }
    
}

