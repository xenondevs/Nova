package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.registryEntrySetOf

/**
 * A binary serializer for [RegistryEntrySet.Nova] that serializes the set by its entries' keys.
 */
class NovaRegistryEntrySetBinarySerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>
) : BinarySerializer<RegistryEntrySet.Nova<T>> {
    
    override fun read(reader: ByteReader): RegistryEntrySet.Nova<T>? {
        return when (val type = reader.readUnsignedByte()) {
            0.toUByte() -> null
            1.toUByte() -> {
                val count = reader.readVarInt()
                val entries = (0..<count).map {
                    val key = Key.key(reader.readString())
                    registry[key]
                }
                registryEntrySetOf(entries)
            }
            
            2.toUByte() -> {
                val tagKey = Key.key(reader.readString())
                registry.getTag(tagKey)
            }
            
            else -> throw IllegalArgumentException("Unknown RegistryEntrySet type: $type")
        }
    }
    
    override fun write(obj: RegistryEntrySet.Nova<T>?, writer: ByteWriter) {
        when (obj) {
            null -> writer.writeUnsignedByte(0U)
            
            is RegistryEntrySet.Nova.Direct<T> -> {
                writer.writeUnsignedByte(1U)
                writer.writeVarInt(obj.entries.size)
                for (entry in obj.entries) {
                    writer.writeString(entry.key.asString())
                }
            }
            
            is RegistryEntrySet.Nova.Tag<T> -> {
                writer.writeUnsignedByte(2U)
                writer.writeString(obj.tagKey.asString())
            }
        }
    }
    
    override fun copy(obj: RegistryEntrySet.Nova<T>?): RegistryEntrySet.Nova<T>? = obj
    
}

/**
 * A binary serializer for [RegistryEntrySet.Paper] that serializes the set by its entries' keys.
 */
class PaperRegistryEntrySetBinarySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : BinarySerializer<RegistryEntrySet.Paper<T>> {
    
    override fun read(reader: ByteReader): RegistryEntrySet.Paper<T>? {
        return when (val type = reader.readUnsignedByte()) {
            0.toUByte() -> null
            1.toUByte() -> {
                val count = reader.readVarInt()
                val entries = (0..<count).map {
                    val key = Key.key(reader.readString())
                    TypedKey.create(registryKey, key)
                }
                registryEntrySetOf(entries, registryAccess)
            }
            
            2.toUByte() -> {
                val key = Key.key(reader.readString())
                registryEntrySetOf(TagKey.create(registryKey, key), registryAccess)
            }
            
            else -> throw IllegalArgumentException("Unknown RegistryEntrySet type: $type")
        }
    }
    
    override fun write(obj: RegistryEntrySet.Paper<T>?, writer: ByteWriter) {
        when (obj) {
            null -> writer.writeUnsignedByte(0U)
            
            is RegistryEntrySet.Paper.Direct<T> -> {
                writer.writeUnsignedByte(1U)
                writer.writeVarInt(obj.entries.size)
                for (entry in obj.entries) {
                    writer.writeString(entry.key.asString())
                }
            }
            
            is RegistryEntrySet.Paper.Tag<T> -> {
                writer.writeUnsignedByte(2U)
                writer.writeString(obj.tagKey.key().asString())
            }
        }
    }
    
    override fun copy(obj: RegistryEntrySet.Paper<T>?): RegistryEntrySet.Paper<T>? = obj
    
}

/**
 * A binary serializer for [RegistryEntrySet.Mixed] that serializes the set by its entries' keys.
 *
 * Nova registries will be prioritized for direct entries.
 * Tags will be merged from both registries.
 */
class MixedRegistryEntrySetBinarySerializer<N : NovaRegistryElement<N>, P : Keyed>(
    private val novaRegistry: NovaRegistry<N>,
    private val paperRegistry: RegistryKey<P>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : BinarySerializer<RegistryEntrySet.Mixed<N, P>> {
    
    override fun read(reader: ByteReader): RegistryEntrySet.Mixed<N, P>? {
        return when (val type = reader.readUnsignedByte()) {
            0.toUByte() -> null
            1.toUByte() -> {
                val count = reader.readVarInt()
                val entries = (0..<count).map {
                    val key = Key.key(reader.readString())
                    RegistryEntry.either(key, novaRegistry, paperRegistry, registryAccess)
                }
                registryEntrySetOf(entries, novaRegistry, paperRegistry)
            }
            
            2.toUByte() -> {
                val tagKey = Key.key(reader.readString())
                registryEntrySetOf(tagKey, novaRegistry, paperRegistry, registryAccess)
            }
            
            else -> throw IllegalArgumentException("Unknown RegistryEntrySet type: $type")
        }
    }
    
    override fun write(obj: RegistryEntrySet.Mixed<N, P>?, writer: ByteWriter) {
        when (obj) {
            null -> writer.writeUnsignedByte(0U)
            
            is RegistryEntrySet.Mixed.Direct<N, P> -> {
                writer.writeUnsignedByte(1U)
                writer.writeVarInt(obj.entries.size)
                for (entry in obj.entries) {
                    writer.writeString(entry.key.asString())
                }
            }
            
            is RegistryEntrySet.Mixed.Tag<N, P> -> {
                writer.writeUnsignedByte(2U)
                writer.writeString(obj.tagKey.key().asString())
            }
        }
    }
    
    override fun copy(obj: RegistryEntrySet.Mixed<N, P>?): RegistryEntrySet.Mixed<N, P>? = obj
    
}
