package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import org.bukkit.Bukkit
import org.bukkit.Location
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.util.data.writeUUID
import java.lang.reflect.Type

internal object LocationBinaryAdapterLegacy : BinaryAdapterLegacy<Location> {
    
    override fun write(obj: Location, buf: ByteBuf) {
        val world = obj.world
        if (world != null) {
            buf.writeBoolean(true)
            buf.writeUUID(world.uid)
        } else buf.writeBoolean(false)
        
        buf.writeDouble(obj.x)
        buf.writeDouble(obj.y)
        buf.writeDouble(obj.z)
        buf.writeFloat(obj.yaw)
        buf.writeFloat(obj.pitch)
    }
    
    override fun read(type: Type, buf: ByteBuf): Location {
        return Location(
            if (buf.readBoolean()) buf.readUUID().let(Bukkit::getWorld) else null,
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readFloat()
        )
    }
    
}