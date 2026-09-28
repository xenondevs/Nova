package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.nova.world.*

import org.bukkit.Bukkit
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import org.bukkit.block.Block

internal object BlockPosBinarySerializer : UnversionedBinarySerializer<Block>() {
    
    override fun copyNonNull(obj: Block): Block {
        return obj
    }
    
    override fun readUnversioned(reader: ByteReader): Block {
        val worldUuid = reader.readUUID()
        val world = Bukkit.getWorld(worldUuid) ?: throw IllegalStateException("No world with UUID $worldUuid found")
        val x = reader.readVarInt()
        val y = reader.readVarInt()
        val z = reader.readVarInt()
        return world.getBlockAt(x, y, z)
    }
    
    override fun writeUnversioned(obj: Block, writer: ByteWriter) {
        writer.writeUUID(obj.world.uid)
        writer.writeVarInt(obj.x)
        writer.writeVarInt(obj.y)
        writer.writeVarInt(obj.z)
    }
    
}
