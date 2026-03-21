package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer

/**
 * A binary serializer for [RegistryKeySet] that serializes the set by its entries' keys.
 */
class RegistryKeySetBinarySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : BinarySerializer<RegistryKeySet<T>> {
    
    private val registry by lazy { registryAccess.getRegistry(registryKey) }
    
    override fun read(reader: ByteReader): RegistryKeySet<T>? {
        return when (val type = reader.readUnsignedByte()) {
            0.toUByte() -> null
            1.toUByte() -> {
                val count = reader.readVarInt()
                val keys = (0..<count).map {
                    val key = Key.key(reader.readString())
                    TypedKey.create(registryKey, key)
                }
                RegistrySet.keySet(registryKey, keys)
            }
            
            2.toUByte() -> {
                val key = Key.key(reader.readString())
                registry.getTag(TagKey.create(registryKey, key))
            }
            
            else -> throw IllegalArgumentException("Unknown RegistryKeySet type: $type")
        }
    }
    
    override fun write(obj: RegistryKeySet<T>?, writer: ByteWriter) {
        when (obj) {
            null -> writer.writeUnsignedByte(0U)
            
            is io.papermc.paper.registry.tag.Tag<T> -> {
                writer.writeUnsignedByte(2U)
                writer.writeString(obj.tagKey().key().asString())
            }
            
            else -> {
                writer.writeUnsignedByte(1U)
                val values = obj.values()
                writer.writeVarInt(values.size)
                for (value in values) {
                    writer.writeString(value.key().asString())
                }
            }
        }
    }
    
    override fun copy(obj: RegistryKeySet<T>?): RegistryKeySet<T>? = obj
    
}


