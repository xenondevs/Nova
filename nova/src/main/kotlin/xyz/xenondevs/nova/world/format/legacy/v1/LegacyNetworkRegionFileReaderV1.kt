package xyz.xenondevs.nova.world.format.legacy.v1

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import net.kyori.adventure.key.Key
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.CompressionType
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkRegionFile
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import xyz.xenondevs.nova.world.format.legacy.LegacyRegionizedFileReader
import java.util.*

private const val NUM_CHUNKS = 1024

internal object LegacyNetworkRegionFileReaderV1 : LegacyRegionizedFileReader<NetworkChunk, NetworkRegionFile> {
    
    override fun read(reader: ByteReader, world: World, regionX: Int, regionZ: Int): NetworkRegionFile {
        val compressionType = CompressionType.entries[reader.readByte().toInt()]
        compressionType.wrapInput(reader.asInputStream()).use { decompInp ->
            val chunkBitmask = BitSet.valueOf(reader.readBytes(NUM_CHUNKS / 8))
            val decompReader = ByteReader.fromStream(decompInp)
            val chunks = Array(NUM_CHUNKS) { chunkIdx ->
                val pos = chunkIdxToPos(chunkIdx, world, regionX, regionZ)
                if (chunkBitmask.get(chunkIdx))
                    readChunk(pos, decompReader)
                else NetworkChunk()
            }
            return NetworkRegionFile(chunks)
        }
    }
    
    private fun readChunk(pos: ChunkPos, reader: ByteReader): NetworkChunk {
        val bridgeSize = reader.readVarInt()
        val bridges = HashMap<Block, NetworkBridgeData>(bridgeSize)
        repeat(bridgeSize) {
            val block = unpackBlockPos(pos, reader.readInt())
            bridges[block] = readBridgeData(reader)
        }
        
        val endPointSize = reader.readVarInt()
        val endPoints = HashMap<Block, NetworkEndPointData>(endPointSize)
        repeat(endPointSize) {
            val block = unpackBlockPos(pos, reader.readInt())
            endPoints[block] = readEndPointData(reader)
        }
        
        return NetworkChunk(bridges, endPoints)
    }
    
    private fun readBridgeData(reader: ByteReader): NetworkBridgeData =
        NetworkBridgeData(
            Key.key(reader.readString()),
            reader.readUUID(),
            reader.readNetworkTypeCubeFaceSetMap(),
            reader.readNetworkTypeUUIDMap(),
            reader.readNetworkTypeSet(),
            convertLegacyCubeFaceSet(reader.readByte())
        )
    
    private fun readEndPointData(reader: ByteReader): NetworkEndPointData =
        NetworkEndPointData(
            reader.readUUID(),
            reader.readNetworkTypeCubeFaceSetMap(),
            reader.readNetworkTypeBlockFaceUUIDTable()
        )
    
    private fun ByteReader.readNetworkTypeCubeFaceSetMap(): MutableMap<NetworkType<*>, CubeFaceSet> {
        val size = readVarInt()
        val map = HashMap<NetworkType<*>, CubeFaceSet>(size)
        repeat(size) {
            val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
            map[networkType] = convertLegacyCubeFaceSet(readByte())
        }
        return map
    }
    
    private fun ByteReader.readNetworkTypeBlockFaceUUIDTable(): Table<NetworkType<*>, BlockFace, UUID> {
        val size = readVarInt()
        val table = HashBasedTable.create<NetworkType<*>, BlockFace, UUID>()
        repeat(size) {
            val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
            val face = BlockFace.entries[readByte().toInt()]
            table[networkType, face] = readUUID()
        }
        return table
    }
    
    private fun ByteReader.readNetworkTypeUUIDMap(): MutableMap<NetworkType<*>, UUID> {
        val size = readVarInt()
        val map = HashMap<NetworkType<*>, UUID>(size)
        repeat(size) {
            val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
            map[networkType] = readUUID()
        }
        return map
    }
    
    private fun ByteReader.readNetworkTypeSet(): MutableSet<NetworkType<*>> {
        val size = readVarInt()
        val set = HashSet<NetworkType<*>>(size)
        repeat(size) {
            set += NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
        }
        return set
    }
    
    private fun chunkIdxToPos(idx: Int, world: World, regionX: Int, regionZ: Int): ChunkPos {
        val x = regionX shl 5 or (idx shr 5)
        val z = regionZ shl 5 or (idx and 0x1F)
        return ChunkPos(world.uid, x, z)
    }
    
    private fun unpackBlockPos(chunkPos: ChunkPos, value: Int): Block {
        val y = value shr 8
        val x = (value shr 4) and 0xF
        val z = value and 0xF
        return chunkPos.world!!.getBlockAt((chunkPos.x shl 4) + x, y, (chunkPos.z shl 4) + z)
    }
    
}

internal fun convertLegacyCubeFaceSet(data: Byte): CubeFaceSet {
    val reversedData = Integer.reverse(data.toInt() and 0x3F) ushr 26
    return CubeFaceSet(reversedData)
}
