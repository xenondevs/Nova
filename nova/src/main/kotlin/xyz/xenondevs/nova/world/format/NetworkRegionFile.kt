package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import java.io.File

private const val MAGIC = 0x4E564E52 // NVNR
private const val VERSION = 1.toByte()

internal class NetworkRegionFile(
    file: File,
    world: World,
    regionX: Int, regionZ: Int,
    chunks: Array<NetworkChunk>
) : RegionizedFile<NetworkChunk>(MAGIC, VERSION, file, world, regionX, regionZ, chunks) {
    
    companion object : RegionizedFileReader<NetworkChunk, NetworkRegionFile>(MAGIC, VERSION, ::Array, ::NetworkRegionFile, NetworkChunk)
    
}