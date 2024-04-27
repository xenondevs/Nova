package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import java.io.File

internal class NetworkRegionFile(
    file: File,
    world: World,
    regionX: Int, regionZ: Int,
    chunks: Array<NetworkChunk>
) : RegionizedFile<NetworkChunk>(file, world, regionX, regionZ, chunks) {
    
    companion object : RegionizedFileReader<NetworkChunk, NetworkRegionFile>(::Array, ::NetworkRegionFile, NetworkChunk)
    
}