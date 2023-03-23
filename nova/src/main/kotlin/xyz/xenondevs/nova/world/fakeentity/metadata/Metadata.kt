package xyz.xenondevs.nova.world.fakeentity.metadata

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataSerializer
import xyz.xenondevs.nmsutils.network.PacketIdRegistry
import java.util.*

abstract class Metadata internal constructor() {
    
    private val entries = ArrayList<MetadataEntry<*>>()
    
    internal fun packDirty(entityId: Int): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(PacketIdRegistry.CLIENTBOUND_SET_ENTITY_DATA_PACKET)
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
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(PacketIdRegistry.CLIENTBOUND_SET_ENTITY_DATA_PACKET)
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
    
    internal fun <T> entry(index: Int, serializer: EntityDataSerializer<T>, default: T): MetadataEntry<T> {
        val entry = NonNullMetadataEntry(index, serializer, default)
        entries += entry
        return entry
    }
    
    internal fun <C, T> entry(index: Int, serializer: EntityDataSerializer<T>, default: C, mapper: (C) -> T): MetadataEntry<C> {
        val entry = MappedNonNullMetadataEntry(index, serializer, mapper, default)
        entries += entry
        return entry
    }
    
    internal fun <T> optional(index: Int, serializer: EntityDataSerializer<Optional<T & Any>>, default: T?): MetadataEntry<T?> {
        val entry = NullableMetadataEntry(index, serializer, default)
        entries += entry
        return entry
    }
    
    internal fun sharedFlags(index: Int): SharedFlagsMetadataEntry {
        val entry = SharedFlagsMetadataEntry(index)
        entries += entry
        return entry
    }
    
}
