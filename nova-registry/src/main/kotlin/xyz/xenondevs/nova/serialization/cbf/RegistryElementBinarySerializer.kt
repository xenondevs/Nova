package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.Registry
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement

/**
 * A binary serializer for [NovaRegistryElement] that serializes the element by its key.
 */
class NovaRegistryElementBinarySerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>
) : UnversionedBinarySerializer<T>() {
    
    override fun readUnversioned(reader: ByteReader): T {
        val key = Key.key(reader.readString())
        return registry.getValueOrThrow(key)
    }
    
    override fun writeUnversioned(obj: T, writer: ByteWriter) {
        writer.writeString(obj.key.asString())
    }
    
    override fun copyNonNull(obj: T): T {
        return obj
    }
    
}

/**
 * A binary serializer for Paper registry elements that serializes the element by its key.
 * For registry elements that are also an enum, [enumEntries] can be used to provide the enum name mapping as a secondary input format.
 */
class PaperRegistryElementBinarySerializer<T : Keyed>(
    registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess,
    private val enumEntries: Map<String, T> = emptyMap()
) : UnversionedBinarySerializer<T>() {
    
    private val registry: Registry<T> by lazy { registryAccess.getRegistry(registryKey) }
    
    override fun readUnversioned(reader: ByteReader): T {
        val s = reader.readString()
        val enumEntry = enumEntries[s]
        if (enumEntry != null)
            return enumEntry
        
        val key = Key.key(s)
        return registry.getOrThrow(key)
    }
    
    override fun writeUnversioned(obj: T, writer: ByteWriter) {
        writer.writeString(obj.key().asString())
    }
    
    override fun copyNonNull(obj: T): T {
        return obj
    }
    
}
