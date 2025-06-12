package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.Bukkit
import org.bukkit.Location
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

internal object LocationBinarySerializer : UnversionedBinarySerializer<Location>() {
    
    override fun readUnversioned(reader: ByteReader): Location {
        return Location(
            if (reader.readBoolean()) reader.readUUID().let(Bukkit::getWorld) else null,
            reader.readDouble(), reader.readDouble(), reader.readDouble(),
            reader.readFloat(), reader.readFloat()
        )
    }
    
    override fun writeUnversioned(obj: Location, writer: ByteWriter) {
        val world = obj.world
        if (world != null) {
            writer.writeBoolean(true)
            writer.writeUUID(world.uid)
        } else writer.writeBoolean(false)
        
        writer.writeDouble(obj.x)
        writer.writeDouble(obj.y)
        writer.writeDouble(obj.z)
        writer.writeFloat(obj.yaw)
        writer.writeFloat(obj.pitch)
    }
    
    override fun copyNonNull(obj: Location): Location {
        return obj.clone()
    }
    
}