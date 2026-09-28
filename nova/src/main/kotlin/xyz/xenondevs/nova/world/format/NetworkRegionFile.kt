package xyz.xenondevs.nova.world.format

import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import xyz.xenondevs.nova.world.format.legacy.network.v1.LegacyNetworkRegionFileReaderV1

private const val MAGIC = 0x4E564E52 // NVNR
private const val VERSION = 2.toByte()

internal class NetworkRegionFile(chunks: Array<NetworkChunk>) : RegionizedFile<NetworkChunk>(MAGIC, VERSION, chunks) {
    companion object : RegionizedFileReader<NetworkChunk, NetworkRegionFile>(
        MAGIC,
        VERSION,
        ::Array,
        ::NetworkRegionFile,
        NetworkChunk,
        1 to LegacyNetworkRegionFileReaderV1
    )
}
