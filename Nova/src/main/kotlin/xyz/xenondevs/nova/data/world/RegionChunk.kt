package xyz.xenondevs.nova.data.world

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import java.util.logging.Level

internal class RegionChunk(val file: RegionFile, relChunkX: Int, relChunkZ: Int) {
    
    private val chunkX = (file.regionX shl 5) + relChunkX
    private val chunkZ = (file.regionZ shl 5) + relChunkZ
    val packedCoords = (relChunkX shl 5 or relChunkZ).toShort()
    val pos = ChunkPos(file.world.world.uid, chunkX, chunkZ)
    
    var blockStates = HashMap<BlockPos, BlockState>()
    
    fun read(buf: ByteBuf, palette: List<String>) {
        while (buf.readByte().toInt() == 1) { // has next block
            val relPos = buf.readUnsignedByte().toInt()
            val relX = relPos shr 4
            val relZ = relPos and 0xF
            val y = buf.readUnsignedShort()
            val pos = BlockPos(file.world.world, (chunkX shl 4) + relX, y, (chunkZ shl 4) + relZ)
            val type = palette[buf.readInt()]
            
            try {
                val state: BlockState = if (!type.startsWith("minecraft:")) {
                    val material = NovaMaterialRegistry.get(type) as? BlockNovaMaterial
                    if (material == null) {
                        LOGGER.severe("Could not load block at $pos: Invalid id $type")
                        continue
                    }
                    material.createBlockState(pos)
                } else VanillaTileEntityState(pos, type)
                state.read(buf)
                blockStates[pos] = state
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Failed to load block at $pos", e)
            }
        }
    }
    
    fun write(buf: ByteBuf, pool: MutableList<String>): Boolean {
        var changedPool = false
        blockStates.forEach { (pos, state) ->
            if (state is LinkedBlockState) return@forEach
            
            buf.writeByte(1)
            buf.writeByte((pos.x and 0xF shl 4) or (pos.z and 0xF))
            buf.writeShort(pos.y)
            var stateIndex = pool.indexOf(state.id.toString())
            if (stateIndex == -1) {
                pool.add(state.id.toString())
                stateIndex = pool.size - 1
                changedPool = true
            }
            buf.writeInt(stateIndex)
            state.write(buf)
        }
        buf.writeByte(0)
        return changedPool
    }
    
    fun hasData(): Boolean = blockStates.isNotEmpty()
    
}