package xyz.xenondevs.nova.data.serialization.cbf.adapter

import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.data.NamespacedId
import kotlin.reflect.KType

internal object NamespacedKeyBinaryAdapter : BinaryAdapter<NamespacedKey> {
    
    override fun read(type: KType, reader: ByteReader): NamespacedKey {
        return NamespacedKey.fromString(reader.readString())!!
    }
    
    override fun write(obj: NamespacedKey, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}

internal object NamespacedIdBinaryAdapter : BinaryAdapter<NamespacedId> {
    
    override fun read(type: KType, reader: ByteReader): NamespacedId {
        return NamespacedId.of(reader.readString())
    }
    
    override fun write(obj: NamespacedId, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}

internal object ResourceLocationBinaryAdapter : BinaryAdapter<ResourceLocation> {
    
    override fun read(type: KType, reader: ByteReader): ResourceLocation {
        return ResourceLocation.of(reader.readString(), ':')
    }
    
    override fun write(obj: ResourceLocation, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}