package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nmsutils.util.removeIf
import xyz.xenondevs.nova.world.ChunkPos
import java.io.File
import java.util.*

internal class WorldDataStorage(val world: World) {
    
    private val regionsFolder = File(world.worldFolder, "nova_region")
    private val regionFiles: MutableMap<Long, RegionFile> = Collections.synchronizedMap(HashMap())
    
    init {
        regionsFolder.mkdirs()
    }
    
    fun getRegion(pos: ChunkPos): RegionFile {
        val rx = pos.x shr 5
        val rz = pos.z shr 5
        val rid = (rx.toLong() shl 32) or (rz.toLong() and 0xFFFFFFFF)
        
        return regionFiles.getOrPut(rid) {
            val file = File(regionsFolder, "r.$rx.$rz.nvr")
            return@getOrPut RegionFile(this.world, file, rx, rz).apply(RegionFile::init)
        }
    }
    
    fun getRegionOrNull(pos: ChunkPos): RegionFile? {
        val rx = pos.x shr 5
        val rz = pos.z shr 5
        val rid = (rx.toLong() shl 32) or (rz.toLong() and 0xFFFFFFFF)
    
        return regionFiles[rid]
    }
    
    fun saveAll() {
        synchronized(regionFiles) {
            regionFiles.removeIf { (_, regionFile) ->
                regionFile.save()
                return@removeIf !regionFile.isAnyChunkLoaded()
            }
        }
    }
    
}