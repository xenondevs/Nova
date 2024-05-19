package xyz.xenondevs.nova.world.format.chunk

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import net.minecraft.resources.ResourceLocation
import org.bukkit.OfflinePlayer
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.util.getOrThrow
import java.util.*

// TODO: properly handle unknown network types

sealed interface NetworkNodeData {
    
    val owner: UUID
    
    val connections: MutableMap<NetworkType<*>, MutableSet<BlockFace>>
    
    fun write(writer: ByteWriter)
    
}

data class NetworkBridgeData(
    val typeId: ResourceLocation,
    override val owner: UUID,
    override val connections: MutableMap<NetworkType<*>, MutableSet<BlockFace>> = HashMap(),
    val networks: MutableMap<NetworkType<*>, UUID> = HashMap(),
    val supportedNetworkTypes: MutableSet<NetworkType<*>> = HashSet(),
    val bridgeFaces: MutableSet<BlockFace> = enumSet()
) : NetworkNodeData {
    
    constructor(
        typeId: ResourceLocation,
        owner: OfflinePlayer?,
        connections: MutableMap<NetworkType<*>, MutableSet<BlockFace>> = HashMap(),
        networks: MutableMap<NetworkType<*>, UUID> = HashMap(),
        supportedNetworkTypes: MutableSet<NetworkType<*>> = HashSet(),
        bridgeFaces: MutableSet<BlockFace> = enumSet()
    ) : this(
        typeId,
        owner?.uniqueId ?: UUID(0L, 0L), 
        connections, 
        networks, 
        supportedNetworkTypes,
        bridgeFaces
    )
    
    override fun write(writer: ByteWriter) {
        writer.writeString(typeId.toString())
        writer.writeUUID(owner)
        writer.writeNetworkTypeCubeFaceSetMap(connections)
        writer.writeNetworkTypeUUIDMap(networks)
        writer.writeNetworkTypeSet(supportedNetworkTypes)
        writer.writeCubeFaceSet(bridgeFaces)
    }
    
    companion object {
        
        fun read(reader: ByteReader): NetworkBridgeData =
            NetworkBridgeData(
                ResourceLocation(reader.readString()),
                reader.readUUID(),
                reader.readNetworkTypeCubeFaceSetMap(),
                reader.readNetworkTypeUUIDMap(),
                reader.readNetworkTypeSet(),
                reader.readCubeFaceSet()
            )
        
    }
    
}

data class NetworkEndPointData(
    override val owner: UUID,
    override val connections: MutableMap<NetworkType<*>, MutableSet<BlockFace>> = HashMap(),
    val networks: Table<NetworkType<*>, BlockFace, UUID> = HashBasedTable.create()
) : NetworkNodeData {
    
    constructor(
        owner: OfflinePlayer?, 
        connections: MutableMap<NetworkType<*>, MutableSet<BlockFace>> = HashMap(),
        networks: Table<NetworkType<*>, BlockFace, UUID> = HashBasedTable.create()
    ) : this(
        owner?.uniqueId ?: UUID(0L, 0L),
        connections,
        networks
    )
    
    override fun write(writer: ByteWriter) {
        writer.writeUUID(owner)
        writer.writeNetworkTypeCubeFaceSetMap(connections)
        writer.writeNetworkTypeBlockFaceUUIDTable(networks)
    }
    
    companion object {
        
        fun read(reader: ByteReader): NetworkEndPointData =
            NetworkEndPointData(
                reader.readUUID(),
                reader.readNetworkTypeCubeFaceSetMap(),
                reader.readNetworkTypeBlockFaceUUIDTable()
            )
        
    }
    
}

private fun ByteReader.readNetworkTypeCubeFaceSetMap(): MutableMap<NetworkType<*>, MutableSet<BlockFace>> {
    val size = readVarInt()
    val map = HashMap<NetworkType<*>, MutableSet<BlockFace>>(size)
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getOrThrow(readString())
        val set = readCubeFaceSet()
        
        map[networkType] = set
    }
    
    return map
}

private fun ByteWriter.writeNetworkTypeCubeFaceSetMap(map: Map<NetworkType<*>, Set<BlockFace>>) {
    writeVarInt(map.size)
    for ((networkType, set) in map) {
        writeString(networkType.id.toString())
        writeCubeFaceSet(set)
    }
}

private fun ByteReader.readNetworkTypeBlockFaceUUIDTable(): Table<NetworkType<*>, BlockFace, UUID> {
    val size = readVarInt()
    val table = HashBasedTable.create<NetworkType<*>, BlockFace, UUID>()
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getOrThrow(readString())
        val face = BlockFace.entries[readByte().toInt()]
        val uuid = readUUID()
        
        table[networkType, face] = uuid
    }
    
    return table
}

private fun ByteWriter.writeNetworkTypeBlockFaceUUIDTable(table: Table<NetworkType<*>, BlockFace, UUID>) {
    writeVarInt(table.size())
    for ((networkType, face, uuid) in table) {
        writeString(networkType.id.toString())
        writeByte(face.ordinal.toByte())
        writeUUID(uuid)
    }
}

private fun ByteWriter.writeNetworkTypeUUIDMap(map: Map<NetworkType<*>, UUID>) {
    writeVarInt(map.size)
    for ((networkType, uuid) in map) {
        writeString(networkType.id.toString())
        writeUUID(uuid)
    }
}

private fun ByteReader.readNetworkTypeUUIDMap(): MutableMap<NetworkType<*>, UUID> {
    val size = readVarInt()
    val map = HashMap<NetworkType<*>, UUID>(size)
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getOrThrow(readString())
        val uuid = readUUID()
        
        map[networkType] = uuid
    }
    
    return map
}

private fun ByteWriter.writeNetworkTypeSet(set: Set<NetworkType<*>>) {
    writeVarInt(set.size)
    for (networkType in set) {
        writeString(networkType.id.toString())
    }
}

private fun ByteReader.readNetworkTypeSet(): MutableSet<NetworkType<*>> {
    val size = readVarInt()
    val set = HashSet<NetworkType<*>>(size)
    repeat(size) {
        set += NovaRegistries.NETWORK_TYPE.getOrThrow(readString())
    }
    
    return set
}

internal fun ByteWriter.writeCubeFaceSet(set: Set<BlockFace>) {
    var b = 0
    if (BlockFace.NORTH in set)
        b = b or 0b100000
    if (BlockFace.EAST in set)
        b = b or 0b010000
    if (BlockFace.SOUTH in set)
        b = b or 0b001000
    if (BlockFace.WEST in set)
        b = b or 0b000100
    if (BlockFace.UP in set)
        b = b or 0b000010
    if (BlockFace.DOWN in set)
        b = b or 0b000001
    writeByte(b.toByte())
}

internal fun ByteReader.readCubeFaceSet(): MutableSet<BlockFace> {
    val b = readByte().toInt()
    val set = enumSet<BlockFace>()
    if (b and 0b100000 != 0)
        set += BlockFace.NORTH
    if (b and 0b010000 != 0)
        set += BlockFace.EAST
    if (b and 0b001000 != 0)
        set += BlockFace.SOUTH
    if (b and 0b000100 != 0)
        set += BlockFace.WEST
    if (b and 0b000010 != 0)
        set += BlockFace.UP
    if (b and 0b000001 != 0)
        set += BlockFace.DOWN
    return set
}