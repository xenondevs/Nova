package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import xyz.xenondevs.nova.network.PacketIdRegistry
import xyz.xenondevs.nova.util.RegistryFriendlyByteBuf
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.logic.PacketItems
import java.util.*
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

abstract class Metadata internal constructor() {
    
    private val entries = ArrayList<MetadataEntry<*>>()
    
    internal fun packDirty(entityId: Int): FriendlyByteBuf {
        val buf = RegistryFriendlyByteBuf()
        buf.writeVarInt(PacketIdRegistry.PLAY_CLIENTBOUND_SET_ENTITY_DATA)
        buf.writeVarInt(entityId)
        
        entries.forEach {
            if (it.dirty) {
                it.write(buf)
                it.dirty = false
            }
        }
        
        buf.writeByte(0xFF)
        return buf
    }
    
    internal fun pack(entityId: Int): FriendlyByteBuf {
        val buf = RegistryFriendlyByteBuf()
        buf.writeVarInt(PacketIdRegistry.PLAY_CLIENTBOUND_SET_ENTITY_DATA)
        buf.writeVarInt(entityId)
        
        entries.forEach {
            if (it.isNotDefault()) {
                it.write(buf)
                it.dirty = false
            }
        }
        
        buf.writeByte(0xFF)
        return buf
    }
    
    internal fun <T : Any> entry(index: Int, serializer: EntityDataSerializer<T>, default: T): NonNullMetadataEntry<T> {
        val entry = NonNullMetadataEntry(index, serializer, default)
        entries += entry
        return entry
    }
    
    internal fun <T, R : Any> entry(index: Int, serializer: EntityDataSerializer<R>, default: T, map: (T) -> R): MappedNonNullMetadataEntry<T, R> {
        val entry = MappedNonNullMetadataEntry(index, serializer, map, default)
        entries += entry
        return entry
    }
    
    internal fun <T> optional(index: Int, serializer: EntityDataSerializer<Optional<T & Any>>): NullableMetadataEntry<T> {
        val entry = NullableMetadataEntry<T>(index, serializer)
        entries += entry
        return entry
    }
    
    internal fun <T, R> optional(index: Int, serializer: EntityDataSerializer<Optional<R & Any>>, map: (T) -> R): MappedNullableMetadataEntry<T, R> {
        val entry = MappedNullableMetadataEntry(index, serializer, map)
        entries += entry
        return entry
    }
    
    internal fun sharedFlags(index: Int): SharedFlagsMetadataEntry {
        val entry = SharedFlagsMetadataEntry(index)
        entries += entry
        return entry
    }
    
    internal fun itemStack(index: Int, useName: Boolean, default: BukkitStack? = null): MappedNonNullMetadataEntry<BukkitStack?, MojangStack> {
        val entry = MappedNonNullMetadataEntry<BukkitStack?, MojangStack>(
            index, EntityDataSerializers.ITEM_STACK,
            { PacketItems.getClientSideStack(null, it.unwrap().copy(), useName) },
            default
        )
        entries += entry
        return entry
    }
    
}
