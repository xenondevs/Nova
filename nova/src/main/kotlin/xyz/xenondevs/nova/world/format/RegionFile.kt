package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.legacy.v1.LegacyRegionFileReaderV1
import java.io.File

private const val MAGIC = 0x004E5652 // .NVR
private const val VERSION = 2.toByte()

internal class RegionFile(
    file: File,
    world: World,
    regionX: Int, regionZ: Int,
    chunks: Array<RegionChunk>
) : RegionizedFile<RegionChunk>(MAGIC, VERSION, file, world, regionX, regionZ, chunks) {
    
    fun isAnyChunkEnabled(): Boolean {
        for (chunk in chunks) {
            if (chunk.isEnabled)
                return true
        }
        return false
    }
    
    companion object : RegionizedFileReader<RegionChunk, RegionFile>(
        MAGIC, VERSION, ::Array, ::RegionFile, RegionChunk,
        1 to LegacyRegionFileReaderV1
    )
    
}