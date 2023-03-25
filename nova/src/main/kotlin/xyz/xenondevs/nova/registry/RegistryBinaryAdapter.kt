package xyz.xenondevs.nova.registry

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import kotlin.reflect.KType

internal class RegistryBinaryAdapter<T>(val registry: Registry<T>): BinaryAdapter<T> {
    
    override fun read(type: KType, reader: ByteReader): T {
        val id = CBF.read<ResourceLocation>(reader)!!
        return registry[id]!!
    }
    
    override fun write(obj: T, type: KType, writer: ByteWriter) {
        val id = registry.getKey(obj)!!
        CBF.write(id, writer)
    }
    
}