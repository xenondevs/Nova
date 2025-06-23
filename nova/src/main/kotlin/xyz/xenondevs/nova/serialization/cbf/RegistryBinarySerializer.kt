package xyz.xenondevs.nova.serialization.cbf

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.util.getValueOrThrow

fun <T : Any> Registry<T>.byNameBinarySerializer(): BinarySerializer<T> {
    return RegistryBinarySerializer(this)
}

internal class RegistryBinarySerializer<T : Any>(private val registry: Registry<T>) : UnversionedBinarySerializer<T>() {
    
    override fun readUnversioned(reader: ByteReader): T {
        val id = ResourceLocation.parse(reader.readString())
        return registry.getValueOrThrow(id)
    }
    
    override fun writeUnversioned(obj: T, writer: ByteWriter) {
        val id = registry.getKey(obj)!!
        writer.writeString(id.toString())
    }
    
    override fun copyNonNull(obj: T): T {
        return obj
    }
    
}