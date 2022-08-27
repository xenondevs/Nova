package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry
import java.lang.reflect.Type

internal object NetworkTypeBinaryAdapter : BinaryAdapter<NetworkType> {
    
    override fun read(type: Type, buf: ByteBuffer): NetworkType {
        return NetworkTypeRegistry.of(buf.readString())!!
    }
    
    override fun write(obj: NetworkType, buf: ByteBuffer) {
        buf.writeString(obj.toString())
    }
    
}