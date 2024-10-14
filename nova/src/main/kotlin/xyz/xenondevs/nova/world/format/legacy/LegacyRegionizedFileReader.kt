package xyz.xenondevs.nova.world.format.legacy

import org.bukkit.World
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.world.format.RegionizedFile
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import java.io.File

internal interface LegacyRegionizedFileReader<C : RegionizedChunk, F : RegionizedFile<C>> {
    
    fun read(file: File, reader: ByteReader, world: World, regionX: Int, regionZ: Int): F
    
}