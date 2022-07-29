package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.Bukkit
import org.bukkit.Location
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import java.lang.reflect.Type

internal object LocationBinaryAdapter : BinaryAdapter<Location> {
    
    override fun read(type: Type, buf: ByteBuffer): Location {
        return Location(
            if (buf.readBoolean()) buf.readUUID().let(Bukkit::getWorld) else null,
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readFloat()
        )
    }
    
    override fun write(obj: Location, buf: ByteBuffer) {
        val world = obj.world
        if(world != null) {
            buf.writeBoolean(true)
            buf.writeUUID(world.uid)
        } else buf.writeBoolean(false)
        
        buf.writeDouble(obj.x)
        buf.writeDouble(obj.y)
        buf.writeDouble(obj.z)
        buf.writeFloat(obj.yaw)
        buf.writeFloat(obj.pitch)
    }
    
}