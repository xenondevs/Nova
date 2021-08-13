package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import java.util.*

class LocationElement(override val value: Location) : BackedElement<Location> {
    override fun getTypeId() = 23
    
    override fun write(buf: ByteBuf) {
        buf.writeDouble(value.x)
        buf.writeDouble(value.y)
        buf.writeDouble(value.z)
        buf.writeFloat(value.yaw)
        buf.writeFloat(value.pitch)
        buf.writeBoolean(value.world != null)
        val world = value.world?.uid ?: return
        buf.writeLong(world.mostSignificantBits)
        buf.writeLong(world.leastSignificantBits)
    }
    
    override fun toString() = value.toString()
    
}

object LocationDeserializer : BinaryDeserializer<LocationElement> {
    override fun read(buf: ByteBuf): LocationElement {
        val x = buf.readDouble()
        val y = buf.readDouble()
        val z = buf.readDouble()
        val yaw = buf.readFloat()
        val pitch = buf.readFloat()
        var world: World? = null
        if (buf.readBoolean())
            world = Bukkit.getWorld(UUID(buf.readLong(), buf.readLong()))
        return LocationElement(Location(world, x, y, z, yaw, pitch))
    }
    
}