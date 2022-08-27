package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry
import xyz.xenondevs.nova.util.data.readStringLegacy
import xyz.xenondevs.nova.util.data.writeStringLegacy
import java.lang.reflect.Type

internal object NetworkTypeBinaryAdapterLegacy : BinaryAdapterLegacy<NetworkType> {
    
    override fun write(obj: NetworkType, buf: ByteBuf) {
        buf.writeStringLegacy(obj.toString())
    }
    
    override fun read(type: Type, buf: ByteBuf): NetworkType {
        return NetworkTypeRegistry.of(buf.readStringLegacy())!!
    }
    
}