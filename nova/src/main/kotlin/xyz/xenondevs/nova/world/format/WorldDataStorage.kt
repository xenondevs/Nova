package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// TODO: RegionFile unloading
internal class WorldDataStorage(val world: World) {
    
    private val regionsFolder = File(world.worldFolder, "nova_region")
    private val regionFiles = ConcurrentHashMap<Long, RegionFile>()
    
    init {
        regionsFolder.mkdirs()
    }
    
    fun getOrLoadRegion(pos: ChunkPos): RegionFile {
        val regionX = pos.x shr 5
        val regionZ = pos.z shr 5
        val regionId = (regionX.toLong() shl 32) or (regionZ.toLong() and 0xFFFFFFFF)
        
        // TODO: region file read should ideally not be inside computeIfAbsent, because it can block other parts of the map
        return regionFiles.computeIfAbsent(regionId) {
            RegionFile.read(File(regionsFolder, "r.$regionX.$regionZ.nvr"), world, regionX, regionZ)
        }
    }
    
    /**
     * Gets a snapshot of all loaded [TileEntities][TileEntity] in this world.
     */
    fun getTileEntities(): List<TileEntity> =
        collectFromChunks { it.getTileEntities() }
    
    /**
     * Gets a snapshot of all loaded [VanillaTileEntities][VanillaTileEntity] in this world.
     */
    fun getVanillaTileEntities(): List<VanillaTileEntity> =
        collectFromChunks { it.getVanillaTileEntities() }
    
    private inline fun <T> collectFromChunks(collector: (RegionChunk) -> List<T>): List<T> {
        val list = ArrayList<T>()
        for (regionFile in regionFiles.values) {
            for (chunk in regionFile.chunks) {
                list += collector(chunk)
            }
        }
        
        return list
    }
    
    /**
     * Saves all loaded [RegionFiles][RegionFile].
     */
    fun saveAllRegions() {
        LOGGER.info("Saving ${world.name} (${regionFiles.size} region files)")
        for (regionFile in regionFiles.values) {
            regionFile.save()
        }
    }
    
    fun disableAllChunks() {
        for (regionFile in regionFiles.values) {
            for (chunk in regionFile.chunks) {
                chunk.disable()
            }
        }
    }
    
}