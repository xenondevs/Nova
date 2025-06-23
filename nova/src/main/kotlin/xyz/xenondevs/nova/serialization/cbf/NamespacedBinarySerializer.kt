@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.serialization.cbf

import net.kyori.adventure.key.Key
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.api.NamespacedId

internal object NamespacedKeyBinarySerializer : UnversionedBinarySerializer<NamespacedKey>() {
    
    override fun readUnversioned(reader: ByteReader): NamespacedKey {
        return NamespacedKey.fromString(reader.readString())!!
    }
    
    override fun writeUnversioned(obj: NamespacedKey, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copyNonNull(obj: NamespacedKey): NamespacedKey {
        return obj
    }
    
}

internal object NamespacedIdBinarySerializer : UnversionedBinarySerializer<NamespacedId>() {
    
    override fun readUnversioned(reader: ByteReader): NamespacedId {
        return NamespacedId.of(reader.readString())
    }
    
    override fun writeUnversioned(obj: NamespacedId, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copyNonNull(obj: NamespacedId): NamespacedId {
        return obj
    }
    
}

internal object ResourceLocationBinarySerializer : UnversionedBinarySerializer<ResourceLocation>() {
    
    override fun readUnversioned(reader: ByteReader): ResourceLocation {
        return ResourceLocation.parse(reader.readString())
    }
    
    override fun writeUnversioned(obj: ResourceLocation, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copyNonNull(obj: ResourceLocation): ResourceLocation {
        return obj
    }
    
}

internal object KeyBinarySerializer : UnversionedBinarySerializer<Key>() {
    
    override fun readUnversioned(reader: ByteReader): Key {
        return Key.key(reader.readString())
    }
    
    override fun writeUnversioned(obj: Key, writer: ByteWriter) {
        writer.writeString(obj.asString())
    }
    
    override fun copyNonNull(obj: Key): Key {
        return obj
    }
    
}