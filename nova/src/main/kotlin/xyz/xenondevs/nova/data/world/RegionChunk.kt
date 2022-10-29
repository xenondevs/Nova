package xyz.xenondevs.nova.data.world

import io.netty.buffer.ByteBuf
import org.bukkit.World
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.adapter.NettyBufferProvider
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.CBFLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.MultiModel
import xyz.xenondevs.nova.tileentity.TileEntityParticleTask
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level

private val DELETE_UNKNOWN_BLOCKS by configReloadable { DEFAULT_CONFIG.getBoolean("world.delete_unknown_blocks") }

internal class RegionChunk(regionX: Int, regionZ: Int, val world: World, relChunkX: Int, relChunkZ: Int) {
    
    val lock = ReentrantReadWriteLock(true)
    
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
                var material: BlockNovaMaterial? = null
                
                if (!isVanillaBlock && (NovaMaterialRegistry.getOrNull(type) as? BlockNovaMaterial)?.also { material = it } == null) {
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
    
    //<editor-fold desc="Legacy function" defaultstate="collapsed">
    
    @Suppress("DuplicatedCode")
    fun readLegacy(buf: ByteBuf, palette: List<String>) {
        while (buf.readByte().toInt() == 1) { // has next block
            val relPos = buf.readUnsignedByte().toInt()
            val relX = relPos shr 4
            val relZ = relPos and 0xF
            val y = buf.readShort().toInt()
            val pos = BlockPos(world, (chunkX shl 4) + relX, y, (chunkZ shl 4) + relZ)
            val type = palette[buf.readInt()]
            
            try {
                val state: BlockState
                if (!type.startsWith("minecraft:")) {
                    val material = NovaMaterialRegistry.get(type) as? BlockNovaMaterial
                    if (material == null) {
                        LOGGER.severe("Could not load block at $pos: Invalid id $type")
                        continue
                    }
                    state = material.createBlockState(pos)
                    state as NovaTileEntityState
                    if (state.properties.isNotEmpty())
                        state.readPropertiesLegacy(buf)
                    state.uuid = buf.readUUID()
                    state.ownerUUID = buf.readUUID()
                    val legacyCompound = CBFLegacy.read<LegacyCompound>(buf)!!
                    state.data = Compound()
                    state.legacyData = legacyCompound
                    val tileEntity = (material as TileEntityNovaMaterial).tileEntityConstructor(state)
                    state.tileEntity = tileEntity
                    tileEntity.multiModels.forEach(MultiModel::close)
                    tileEntity.particleTasks.forEach(TileEntityParticleTask::stop)
                    runCatching { tileEntity.handleRemoved(true) }
                } else {
                    state = VanillaTileEntityState(pos, type)
                    val legacyCompound = CBFLegacy.read<LegacyCompound>(buf)!!
                    val compound = convertVTECompound(legacyCompound)
                    state.data = compound
                }
                blockStates[pos] = state
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Failed to load block at $pos", e)
            }
        }
    }
    
    private fun convertVTECompound(legacyCompound: LegacyCompound): Compound {
        val compound = Compound()
        
        legacyCompound.get<Map<BlockFace, NetworkConnectionType>>("itemConfig")
            ?.let { compound["itemConfig"] = it }
        
        legacyCompound.get<Map<BlockFace, ItemFilter>>("insertFilters")
            ?.let { compound["insertFilters"] = it }
        
        legacyCompound.get<Map<BlockFace, ItemFilter>>("extractFilters")
            ?.let { compound["extractFilters"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("insertPriorities")
            ?.let { compound["insertPriorities"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("extractPriorities")
            ?.let { compound["extractPriorities"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("channels")
            ?.let { compound["channels"] = it }
        
        legacyCompound.get<Map<BlockFace, NetworkConnectionType>>("fluidConnectionConfig")
            ?.let { compound["fluidConnectionConfig"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("fluidInsertPriorities")
            ?.let { compound["fluidInsertPriorities"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("fluidExtractPriorities")
            ?.let { compound["fluidExtractPriorities"] = it }
        
        legacyCompound.get<Map<BlockFace, UUID>>("fluidContainerConfig")
            ?.let { compound["fluidContainerConfig"] = it }
        
        legacyCompound.get<Map<BlockFace, Int>>("fluidChannels")
            ?.let { compound["fluidChannels"] = it }
        
        return compound
    }
    
    // </editor-fold>
    
}