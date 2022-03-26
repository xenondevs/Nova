package xyz.xenondevs.nova.data.world

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileState
import xyz.xenondevs.nova.data.world.block.state.VanillaTileState
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.world.BlockPos

class RegionChunk(val file: RegionFile, relChunkX: Int, relChunkZ: Int) {
    
    private val chunkX = (file.regionX shl 5) + relChunkX
    private val chunkZ = (file.regionZ shl 5) + relChunkZ
    
    var blockStates = HashMap<BlockPos, BlockState>()
    
    fun read(buf: ByteBuf, palette: List<String>) {
        while (buf.readByte().toInt() == 1) { // has next block
            val relPos = buf.readUnsignedByte().toInt()
            val relX = relPos shr 4
            val relZ = relPos and 0xF
            val y = buf.readUnsignedShort()
            val pos = BlockPos(file.world.world, (chunkX shl 4) + relX, y, (chunkZ shl 4) + relZ)
            val type = palette[buf.readInt()]
            
            val state: BlockState = if (!type.startsWith("minecraft:")) {
                val material = NovaMaterialRegistry.get(type) as TileEntityNovaMaterial?
                if (material == null) {
                    LOGGER.severe("Could not load block at $pos: Invalid id $type")
                    continue
                }
                NovaTileState(material)
            } else VanillaTileState(type)
            state.read(buf)
            blockStates[pos] = state
        }
    }
    
    fun write(buf: ByteBuf, palette: List<String>) {
        blockStates.forEach { (pos, state) ->
            buf.writeByte(1)
            buf.writeByte((pos.x and 0xF shl 4) or (pos.z and 0xF))
            buf.writeShort(pos.y)
            buf.writeInt(palette.indexOf(state.id))
            state.write(buf)
        }
        buf.writeByte(0)
    }
    
    fun hasData(): Boolean = blockStates.isNotEmpty()
    
}