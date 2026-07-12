
package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry

/**
 * A binary serializer for [RegistryEntry.Nova] that serializes the entry by its key.
 */
class NovaRegistryEntryBinarySerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>
) : UnversionedBinarySerializer<RegistryEntry.Nova<T>>() {
    
    override fun readUnversioned(reader: ByteReader): RegistryEntry.Nova<T> {
        val key = Key.key(reader.readString())
        return registry[key]
    }
    
    override fun writeUnversioned(obj: RegistryEntry.Nova<T>, writer: ByteWriter) {
        writer.writeString(obj.key.asString())
    }
    
    override fun copyNonNull(obj: RegistryEntry.Nova<T>): RegistryEntry.Nova<T> {
        return obj
    }
    
}

/**
 * A binary serializer for [RegistryEntry.Paper] that serializes the entry by its key.
 */
class PaperRegistryEntryBinarySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : UnversionedBinarySerializer<RegistryEntry.Paper<T>>() {
    
    override fun readUnversioned(reader: ByteReader): RegistryEntry.Paper<T> {
        val key = Key.key(reader.readString())
        return RegistryEntry.paper(TypedKey.create(registryKey, key), registryAccess)
    }
    
    override fun writeUnversioned(obj: RegistryEntry.Paper<T>, writer: ByteWriter) {
        writer.writeString(obj.key.asString())
    }
    
    override fun copyNonNull(obj: RegistryEntry.Paper<T>): RegistryEntry.Paper<T> {
        return obj
    }
    
}

