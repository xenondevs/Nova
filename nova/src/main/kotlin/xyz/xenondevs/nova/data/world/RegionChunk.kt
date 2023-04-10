package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.cbf.adapter.NettyBufferProvider
import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaBlock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level

private val DELETE_UNKNOWN_BLOCKS by configReloadable { DEFAULT_CONFIG.getBoolean("world.delete_unknown_blocks") }

internal class RegionChunk(regionX: Int, regionZ: Int, val world: World, relChunkX: Int, relChunkZ: Int) {
    
    val lock = ReentrantReadWriteLock(true)
    
    @Volatile
    var isLoaded = false
    
    private val chunkX = (regionX shl 5) + relChunkX
    private val chunkZ = (regionZ shl 5) + relChunkZ
    val packedCoords = (relChunkX shl 5 or relChunkZ)
    val pos = ChunkPos(world.uid, chunkX, chunkZ)
    
    var blockStates = HashMap<BlockPos, BlockState>()
    val unknownStates = HashMap<BlockPos, Pair<String, ByteArray>>()
    
    fun read(buf: ByteBuffer) {
        while (buf.readByte().toInt() == 1) {
            val relPos = buf.readUnsignedByte().toInt()
            val relX = relPos shr 4
            val relZ = relPos and 0xF
            val y = buf.readVarInt()
            val pos = BlockPos(world, (chunkX shl 4) + relX, y, (chunkZ shl 4) + relZ)
            val type = buf.readString()
            val dataLength = buf.readVarInt()
            
            try {
                val isVanillaBlock = type.startsWith("minecraft:")
                var material: NovaBlock? = null
                
                if (!isVanillaBlock && NovaRegistries.BLOCK[type]?.also { material = it } == null) {
                    LOGGER.severe("Could not load block at $pos: Invalid id $type")
                    if (DELETE_UNKNOWN_BLOCKS)
                        buf.readBytes(dataLength)
                    else unknownStates[pos] = type to buf.readBytes(dataLength)
                    
                    continue
                }
                
                val state = if (isVanillaBlock) VanillaTileEntityState(pos, type) else material!!.createBlockState(pos)
                state.read(buf)
                blockStates[pos] = state
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Failed to load block at $pos", e)
            }
        }
    }
    
    fun write(buf: ByteBuffer) {
        blockStates.forEach { (pos, state) ->
            if (state is LinkedBlockState) return@forEach
            
            buf.writeByte(1)
            buf.writeByte(((pos.x and 0xF shl 4) or (pos.z and 0xF)).toByte())
            buf.writeVarInt(pos.y)
            buf.writeString(state.id.toString())
            
            val buffer = NettyBufferProvider.getBuffer()
            state.write(buffer)
            buf.writeVarInt(buffer.readableBytes())
            buf.writeBytes(buffer.toByteArray())
        }
        if (!DELETE_UNKNOWN_BLOCKS) {
            unknownStates.forEach { (pos, data) ->
                buf.writeByte(1)
                buf.writeByte(((pos.x and 0xF shl 4) or (pos.z and 0xF)).toByte())
                buf.writeVarInt(pos.y)
                buf.writeString(data.first)
                buf.writeVarInt(data.second.size)
                buf.writeBytes(data.second)
            }
        }
        buf.writeByte(0)
    }
    
    fun isEmpty() = blockStates.isEmpty() && unknownStates.isEmpty()
    
}