package xyz.xenondevs.nova.serialization.cbf.adapter

import org.bukkit.Bukkit
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.BlockPos
import kotlin.reflect.KType

internal object BlockPosBinaryAdapter : BinaryAdapter<BlockPos> {
    
    override fun copy(obj: BlockPos, type: KType): BlockPos {
        return obj.copy()
    }
    
    override fun read(type: KType, reader: ByteReader): BlockPos {
        val worldUuid = reader.readUUID()
        val world = Bukkit.getWorld(worldUuid) ?: throw IllegalStateException("No world with UUID $worldUuid found")
        val x = reader.readVarInt()
        val y = reader.readVarInt()
        val z = reader.readVarInt()
        return BlockPos(world, x, y, z)
    }
    
    override fun write(obj: BlockPos, type: KType, writer: ByteWriter) {
        writer.writeUUID(obj.world.uid)
        writer.writeVarInt(obj.x)
        writer.writeVarInt(obj.y)
        writer.writeVarInt(obj.z)
    }
    
}