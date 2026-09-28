package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

/**
 * A binary serializer for [TypedKey] that serializes to a string of `namespace:value`.
 */
class TypedKeyBinarySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>
) : UnversionedBinarySerializer<TypedKey<T>>() {
    
    override fun readUnversioned(reader: ByteReader): TypedKey<T> {
        val key = Key.key(reader.readString())
        return TypedKey.create(registryKey, key)
    }
    
    override fun writeUnversioned(obj: TypedKey<T>, writer: ByteWriter) {
        writer.writeString(obj.key().asString())
    }
    
    override fun copyNonNull(obj: TypedKey<T>): TypedKey<T> {
        return obj
    }
    
}

