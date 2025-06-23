package xyz.xenondevs.nova.world.format

import xyz.xenondevs.nova.world.format.chunk.NetworkChunk

private const val MAGIC = 0x4E564E52 // NVNR
private const val VERSION = 1.toByte()

internal class NetworkRegionFile(chunks: Array<NetworkChunk>) : RegionizedFile<NetworkChunk>(MAGIC, VERSION, chunks) {
    companion object : RegionizedFileReader<NetworkChunk, NetworkRegionFile>(MAGIC, VERSION, ::Array, ::NetworkRegionFile, NetworkChunk)
}