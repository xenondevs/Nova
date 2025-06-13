package xyz.xenondevs.nova.world.format

import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.legacy.v1.LegacyRegionFileReaderV1

private const val MAGIC = 0x004E5652 // .NVR
private const val VERSION = 2.toByte()

internal class RegionFile(chunks: Array<RegionChunk>) : RegionizedFile<RegionChunk>(MAGIC, VERSION, chunks) {
    
    fun isInactive(): Boolean {
        for (chunk in chunks) {
            if (chunk.isEnabled || !chunk.hasBeenEnabled) // fixme: !chunk.hasBeenEnabled check creates memory leak
                return false
        }
        return true
    }
    
    companion object : RegionizedFileReader<RegionChunk, RegionFile>(
        MAGIC, VERSION, ::Array, ::RegionFile, RegionChunk,
        1 to LegacyRegionFileReaderV1
    )
    
}