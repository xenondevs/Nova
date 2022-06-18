package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import java.lang.reflect.Type

internal object NetworkTypeBinaryAdapter : BinaryAdapter<NetworkType> {
    
    override fun write(obj: NetworkType, buf: ByteBuf) {
        buf.writeString(obj.toString())
    }
    
    override fun read(type: Type, buf: ByteBuf): NetworkType {
        return NetworkTypeRegistry.of(buf.readString())!!
    }
    
}