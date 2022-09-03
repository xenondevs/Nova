package xyz.xenondevs.nova.world.fakeentity.metadata

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf

abstract class Metadata internal constructor() {
    
    private val entries = ArrayList<MetadataEntry<*>>()
    
    internal fun packDirty(entityId: Int): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x50)
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
        buf.writeVarInt(0x50)
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
    
    internal fun <T> entry(index: Int, serializer: MetadataSerializer<T>, default: T): MetadataEntry<T> {
        val entry = MetadataEntry(index, serializer, default)
        entries += entry
        return entry
    }
    
    internal fun sharedFlags(index: Int): SharedFlagsMetadataEntry {
        val entry = SharedFlagsMetadataEntry(index)
        entries += entry
        return entry
    }
    
}
