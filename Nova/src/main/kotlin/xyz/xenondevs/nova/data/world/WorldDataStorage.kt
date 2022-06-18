package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.world.ChunkPos
import java.io.File

internal class WorldDataStorage(val world: World) {
    
    private val regionsFolder = File(world.worldFolder, "nova_region")
    private val regionFiles = HashMap<Long, RegionFile>()
    
    init {
        regionsFolder.mkdirs()
    }
    
    fun getRegion(pos: ChunkPos): RegionFile {
        val rx = pos.x shr 5
        val rz = pos.z shr 5
        
        val rid = (rx.toLong() shl 32) or (rz.toLong() and 0xFFFFFFFF)
        
        return regionFiles.getOrPut(rid) {
            val file = File(regionsFolder, "r.$rx.$rz.nvr")
            return@getOrPut RegionFile(this, file, rx, rz).apply(RegionFile::init)
        }
    }
    
    fun saveAll() {
        regionFiles.removeIf { (_, regionFile) ->
            regionFile.saveAll()
            return@removeIf regionFile.chunks.all { it == null }
        }
    }
    
}