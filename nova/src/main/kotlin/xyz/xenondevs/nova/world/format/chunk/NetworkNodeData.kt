package xyz.xenondevs.nova.world.format.chunk

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import net.kyori.adventure.key.Key
import org.bukkit.OfflinePlayer
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import java.util.*

sealed interface NetworkNodeData {
    
    val owner: UUID
    
    val connections: MutableMap<NetworkType<*>, CubeFaceSet>
    
    fun write(writer: ByteWriter)
    
}

data class NetworkBridgeData(
    val typeId: Key,
    override val owner: UUID,
    override val connections: MutableMap<NetworkType<*>, CubeFaceSet> = HashMap(),
    val networks: MutableMap<NetworkType<*>, UUID> = HashMap(),
    val supportedNetworkTypes: MutableSet<NetworkType<*>> = HashSet(),
    val bridgeFaces: CubeFaceSet = CubeFaceSet.NONE
) : NetworkNodeData {
    
    constructor(
        typeId: Key,
        owner: OfflinePlayer?,
        connections: MutableMap<NetworkType<*>, CubeFaceSet> = HashMap(),
        networks: MutableMap<NetworkType<*>, UUID> = HashMap(),
        supportedNetworkTypes: MutableSet<NetworkType<*>> = HashSet(),
        bridgeFaces: CubeFaceSet = CubeFaceSet.NONE
    ) : this(
        typeId,
        owner?.uniqueId ?: UUID(0L, 0L),
        connections,
        networks,
        supportedNetworkTypes,
        bridgeFaces
    )
    
    override fun write(writer: ByteWriter) {
        writer.writeString(typeId.asString())
        writer.writeUUID(owner)
        writer.writeNetworkTypeCubeFaceSetMap(connections)
        writer.writeNetworkTypeUUIDMap(networks)
        writer.writeNetworkTypeSet(supportedNetworkTypes)
        // FIXME !!!!!!!! LEGACY CONVERSION: BIT ORDER IS NOW REVERSED
        writer.writeByte(bridgeFaces.data)
    }
    
    companion object {
        
        fun read(reader: ByteReader): NetworkBridgeData =
            NetworkBridgeData(
                Key.key(reader.readString()),
                reader.readUUID(),
                reader.readNetworkTypeCubeFaceSetMap(),
                reader.readNetworkTypeUUIDMap(),
                reader.readNetworkTypeSet(),
                // FIXME !!!!!!!! LEGACY CONVERSION: BIT ORDER IS NOW REVERSED
                CubeFaceSet(reader.readByte())
            )
        
    }
    
}

data class NetworkEndPointData(
    override val owner: UUID,
    override val connections: MutableMap<NetworkType<*>, CubeFaceSet> = HashMap(),
    val networks: Table<NetworkType<*>, BlockFace, UUID> = HashBasedTable.create()
) : NetworkNodeData {
    
    constructor(
        owner: OfflinePlayer?,
        connections: MutableMap<NetworkType<*>, CubeFaceSet> = HashMap(),
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

private fun ByteReader.readNetworkTypeCubeFaceSetMap(): MutableMap<NetworkType<*>, CubeFaceSet> {
    val size = readVarInt()
    val map = HashMap<NetworkType<*>, CubeFaceSet>(size)
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
        val set = CubeFaceSet(readByte())
        
        map[networkType] = set
    }
    
    return map
}

private fun ByteWriter.writeNetworkTypeCubeFaceSetMap(map: Map<NetworkType<*>, CubeFaceSet>) {
    writeVarInt(map.size)
    for ([networkType, set] in map) {
        writeString(networkType.key.asString())
        // FIXME !!!!!!!! LEGACY CONVERSION: BIT ORDER IS NOW REVERSED
        writeByte(set.data)
    }
}

private fun ByteReader.readNetworkTypeBlockFaceUUIDTable(): Table<NetworkType<*>, BlockFace, UUID> {
    val size = readVarInt()
    val table = HashBasedTable.create<NetworkType<*>, BlockFace, UUID>()
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
        val face = BlockFace.entries[readByte().toInt()]
        val uuid = readUUID()
        
        table[networkType, face] = uuid
    }
    
    return table
}

private fun ByteWriter.writeNetworkTypeBlockFaceUUIDTable(table: Table<NetworkType<*>, BlockFace, UUID>) {
    writeVarInt(table.size())
    for ([networkType, face, uuid] in table) {
        writeString(networkType.key.asString())
        writeByte(face.ordinal.toByte())
        writeUUID(uuid)
    }
}

private fun ByteWriter.writeNetworkTypeUUIDMap(map: Map<NetworkType<*>, UUID>) {
    writeVarInt(map.size)
    for ([networkType, uuid] in map) {
        writeString(networkType.key.asString())
        writeUUID(uuid)
    }
}

private fun ByteReader.readNetworkTypeUUIDMap(): MutableMap<NetworkType<*>, UUID> {
    val size = readVarInt()
    val map = HashMap<NetworkType<*>, UUID>(size)
    repeat(size) {
        val networkType = NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
        val uuid = readUUID()
        
        map[networkType] = uuid
    }
    
    return map
}

private fun ByteWriter.writeNetworkTypeSet(set: Set<NetworkType<*>>) {
    writeVarInt(set.size)
    for (networkType in set) {
        writeString(networkType.key.asString())
    }
}

private fun ByteReader.readNetworkTypeSet(): MutableSet<NetworkType<*>> {
    val size = readVarInt()
    val set = HashSet<NetworkType<*>>(size)
    repeat(size) {
        set += NovaRegistries.NETWORK_TYPE.getValueOrThrow(Key.key(readString()))
    }
    
    return set
}
