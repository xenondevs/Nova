package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.nbt.CompoundTag
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.util.data.getByteArrayOrNull
import xyz.xenondevs.nova.util.data.getCompoundOrNull

internal data object ItemStackNamespacedCompoundConverter {
    
    private const val LEGACY_DATA_KEY = "nova_cbf"
    private const val PERSISTENT_DATA_KEY = "PublicBukkitValues"
    
    /**
     * Moves the individually serialized values of the legacy namespaced compound into the
     * Bukkit persistent data container.
     */
    fun convert(tag: CompoundTag): Boolean {
        val serializedCompound = tag.getByteArrayOrNull(LEGACY_DATA_KEY)
            ?: return false
        val reader = ByteReader.fromByteArray(serializedCompound)
        var persistentData = tag.getCompoundOrNull(PERSISTENT_DATA_KEY)
        
        if (reader.readUnsignedByte() != 0.toUByte()) {
            repeat(reader.readVarInt()) namespace@{
                val namespace = reader.readString()
                val compoundVersion = reader.readUnsignedByte()
                if (compoundVersion == 0.toUByte())
                    return@namespace
                
                repeat(reader.readVarInt()) entry@{
                    val key = reader.readString()
                    val value = reader.readBytes(reader.readVarInt())
                    
                    // Compound v1 serialized null entries instead of omitting them.
                    if (compoundVersion == 1.toUByte() && value.size == 1 && value[0] == 0.toByte())
                        return@entry
                    
                    val data = persistentData ?: CompoundTag().also {
                        persistentData = it
                        tag.put(PERSISTENT_DATA_KEY, it)
                    }
                    data.putByteArray("$namespace:$key", value)
                }
            }
        }
        
        tag.remove(LEGACY_DATA_KEY)
        return true
    }
    
}