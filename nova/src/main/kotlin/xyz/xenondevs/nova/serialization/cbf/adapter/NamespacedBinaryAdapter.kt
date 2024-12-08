@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.serialization.cbf.adapter

import net.kyori.adventure.key.Key
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.api.NamespacedId
import kotlin.reflect.KType

internal object NamespacedKeyBinaryAdapter : BinaryAdapter<NamespacedKey> {
    
    override fun read(type: KType, reader: ByteReader): NamespacedKey {
        return NamespacedKey.fromString(reader.readString())!!
    }
    
    override fun write(obj: NamespacedKey, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copy(obj: NamespacedKey, type: KType): NamespacedKey {
        return obj
    }
    
}

internal object NamespacedIdBinaryAdapter : BinaryAdapter<NamespacedId> {
    
    override fun read(type: KType, reader: ByteReader): NamespacedId {
        return NamespacedId.of(reader.readString())
    }
    
    override fun write(obj: NamespacedId, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copy(obj: NamespacedId, type: KType): NamespacedId {
        return obj
    }
    
}

internal object ResourceLocationBinaryAdapter : BinaryAdapter<ResourceLocation> {
    
    override fun read(type: KType, reader: ByteReader): ResourceLocation {
        return ResourceLocation.parse(reader.readString())
    }
    
    override fun write(obj: ResourceLocation, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copy(obj: ResourceLocation, type: KType): ResourceLocation {
        return obj
    }
    
}

internal object KeyBinaryAdapter : BinaryAdapter<Key> {
    
    override fun read(type: KType, reader: ByteReader): Key {
        return Key.key(reader.readString())
    }
    
    override fun write(obj: Key, type: KType, writer: ByteWriter) {
        writer.writeString(obj.asString())
    }
    
    override fun copy(obj: Key, type: KType): Key {
        return obj
    }
    
}