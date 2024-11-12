package xyz.xenondevs.nova.serialization.cbf.adapter

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.util.getValueOrThrow
import kotlin.reflect.KType

fun <T : Any> Registry<T>.byNameBinaryAdapter(): BinaryAdapter<T> {
    return RegistryBinaryAdapter(this)
}

internal class RegistryBinaryAdapter<T : Any>(private val registry: Registry<T>) : BinaryAdapter<T> {
    
    override fun read(type: KType, reader: ByteReader): T {
        val id = ResourceLocation.parse(reader.readString())
        return registry.getValueOrThrow(id)
    }
    
    override fun write(obj: T, type: KType, writer: ByteWriter) {
        val id = registry.getKey(obj)!!
        writer.writeString(id.toString())
    }
    
    override fun copy(obj: T, type: KType): T {
        return obj
    }
    
}