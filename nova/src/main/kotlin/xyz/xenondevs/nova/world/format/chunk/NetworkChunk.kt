package xyz.xenondevs.nova.world.format.chunk

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk.Companion.packBlockPos

internal class NetworkChunk(
    private val bridges: MutableMap<BlockPos, NetworkBridgeData> = HashMap(),
    private val endPoints: MutableMap<BlockPos, NetworkEndPointData> = HashMap()
) : RegionizedChunk {
    
    fun getData(): Map<BlockPos, NetworkNodeData> {
        val map = HashMap<BlockPos, NetworkNodeData>(bridges.size + endPoints.size)
        map.putAll(bridges)
        map.putAll(endPoints)
        return map
    }
    
    fun getData(pos: BlockPos): NetworkNodeData? =
        bridges[pos] ?: endPoints[pos]
    
    fun getBridgeData(pos: BlockPos): NetworkBridgeData? =
        bridges[pos]
    
    fun getEndPointData(pos: BlockPos): NetworkEndPointData? =
        endPoints[pos]
    
    fun setData(pos: BlockPos, data: NetworkNodeData?) {
        when (data) {
            is NetworkBridgeData -> {
                bridges[pos] = data
                endPoints.remove(pos)
            }
            
            is NetworkEndPointData -> {
                endPoints[pos] = data
                endPoints.remove(pos)
            }
            
            else -> {
                bridges.remove(pos)
                endPoints.remove(pos)
            }
        }
    }
    
    fun setBridgeData(pos: BlockPos, data: NetworkBridgeData?) {
        if (data != null) {
            bridges[pos] = data
        } else {
            bridges.remove(pos)
        }
    }
    
    fun setEndPointData(pos: BlockPos, data: NetworkEndPointData?) {
        if (data != null) {
            endPoints[pos] = data
        } else {
            endPoints.remove(pos)
        }
    }
    
    override fun write(writer: ByteWriter): Boolean {
        if (bridges.isEmpty() && endPoints.isEmpty())
            return false
        
        writer.writeVarInt(bridges.size)
        for ((pos, data) in bridges) {
            writer.writeInt(packBlockPos(pos))
            data.write(writer)
        }
        
        writer.writeVarInt(endPoints.size)
        for ((pos, data) in endPoints) {
            writer.writeInt(packBlockPos(pos))
            data.write(writer)
        }
        
        return true
    }
    
    companion object : RegionizedChunkReader<NetworkChunk>() {
        
        override fun read(pos: ChunkPos, reader: ByteReader): NetworkChunk {
            val bridgeSize = reader.readVarInt()
            val bridges = HashMap<BlockPos, NetworkBridgeData>(bridgeSize)
            repeat(bridgeSize) {
                val blockPos = unpackBlockPos(pos, reader.readInt())
                val data = NetworkBridgeData.read(reader)
                bridges[blockPos] = data
            }
            
            val endPointSize = reader.readVarInt()
            val endPoints = HashMap<BlockPos, NetworkEndPointData>(endPointSize)
            repeat(endPointSize) {
                val blockPos = unpackBlockPos(pos, reader.readInt())
                val data = NetworkEndPointData.read(reader)
                endPoints[blockPos] = data
            }
            
            return NetworkChunk(bridges, endPoints)
        }
        
        override fun createEmpty(pos: ChunkPos): NetworkChunk {
            return NetworkChunk()
        }
        
    }
    
}