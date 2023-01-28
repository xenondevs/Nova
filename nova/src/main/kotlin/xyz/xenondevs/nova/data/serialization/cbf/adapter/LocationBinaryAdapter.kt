package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.Bukkit
import org.bukkit.Location
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import java.lang.reflect.Type

internal object LocationBinaryAdapter : BinaryAdapter<Location> {
    
    override fun read(type: Type, reader: ByteReader): Location {
        return Location(
            if (reader.readBoolean()) reader.readUUID().let(Bukkit::getWorld) else null,
            reader.readDouble(), reader.readDouble(), reader.readDouble(),
            reader.readFloat(), reader.readFloat()
        )
    }
    
    override fun write(obj: Location, writer: ByteWriter) {
        val world = obj.world
        if(world != null) {
            writer.writeBoolean(true)
            writer.writeUUID(world.uid)
        } else writer.writeBoolean(false)
        
        writer.writeDouble(obj.x)
        writer.writeDouble(obj.y)
        writer.writeDouble(obj.z)
        writer.writeFloat(obj.yaw)
        writer.writeFloat(obj.pitch)
    }
    
}