package xyz.xenondevs.nova.registry

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import kotlin.reflect.KType

internal class RegistryBinaryAdapter<T : Any>(private val registry: Registry<T>): BinaryAdapter<T> {
    
    override fun read(type: KType, reader: ByteReader): T {
        val id = ResourceLocation.of(reader.readString(), ':')
        return registry[id]!!
    }
    
    override fun write(obj: T, type: KType, writer: ByteWriter) {
        val id = registry.getKey(obj)!!
        writer.writeString(id.toString())
    }
    
    override fun copy(obj: T, type: KType): T {
        return obj
    }
    
}