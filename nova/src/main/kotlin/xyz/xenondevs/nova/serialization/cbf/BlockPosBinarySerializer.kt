package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.Bukkit
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.world.BlockPos

internal object BlockPosBinarySerializer : UnversionedBinarySerializer<BlockPos>() {
    
    override fun copyNonNull(obj: BlockPos): BlockPos {
        return obj.copy()
    }
    
    override fun readUnversioned(reader: ByteReader): BlockPos {
        val worldUuid = reader.readUUID()
        val world = Bukkit.getWorld(worldUuid) ?: throw IllegalStateException("No world with UUID $worldUuid found")
        val x = reader.readVarInt()
        val y = reader.readVarInt()
        val z = reader.readVarInt()
        return BlockPos(world, x, y, z)
    }
    
    override fun writeUnversioned(obj: BlockPos, writer: ByteWriter) {
        writer.writeUUID(obj.world.uid)
        writer.writeVarInt(obj.x)
        writer.writeVarInt(obj.y)
        writer.writeVarInt(obj.z)
    }
    
}