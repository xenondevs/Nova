package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import java.io.File

internal class RegionFile(
    file: File,
    world: World,
    regionX: Int, regionZ: Int,
    chunks: Array<RegionChunk>
) : RegionizedFile<RegionChunk>(file, world, regionX, regionZ, chunks) {
    
    fun isAnyChunkEnabled(): Boolean {
        for (chunk in chunks) {
            if (chunk.isEnabled)
                return true
        }
        return false
    }
    
    fun isAnyChunkLoaded(): Boolean {
        val chunkSource = world.serverLevel.chunkSource
        val offsetX = regionX shl 5
        val offsetZ = regionZ shl 5
        repeat(1024) {
            if (chunkSource.isChunkLoaded(offsetX or (it shr 5), offsetZ or (it and 0x1F)))
                return true
        }
        return false
    }
    
    companion object : RegionizedFileReader<RegionChunk, RegionFile>(::Array, ::RegionFile, RegionChunk)
    
}