package xyz.xenondevs.nova.world.format.legacy.world.v2

import xyz.xenondevs.nova.world.format.RegionizedFile
import xyz.xenondevs.nova.world.format.RegionizedFileReader
import xyz.xenondevs.nova.world.format.chunk.LegacyRegionChunk

private const val MAGIC = 0x004E5652 // .NVR
private const val VERSION = 2.toByte()

internal class LegacyRegionFile(chunks: Array<LegacyRegionChunk>) : RegionizedFile<LegacyRegionChunk>(MAGIC, VERSION, chunks) {
    class Reader(blockStateIdResolver: LegacyBlockStateIdResolver) : RegionizedFileReader<LegacyRegionChunk, LegacyRegionFile>(
        MAGIC, VERSION, ::Array, ::LegacyRegionFile, LegacyRegionChunk.Reader(blockStateIdResolver)
    )
}
