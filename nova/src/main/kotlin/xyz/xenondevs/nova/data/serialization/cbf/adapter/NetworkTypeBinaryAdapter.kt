package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry
import kotlin.reflect.KType

internal object NetworkTypeBinaryAdapter : BinaryAdapter<NetworkType> {
    
    override fun read(type: KType, reader: ByteReader): NetworkType {
        return NetworkTypeRegistry.of(reader.readString())!!
    }
    
    override fun write(obj: NetworkType, type: KType, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
}