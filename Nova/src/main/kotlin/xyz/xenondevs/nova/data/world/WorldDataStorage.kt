package xyz.xenondevs.nova.data.world

import io.netty.buffer.Unpooled
import org.bukkit.World
import xyz.xenondevs.nova.util.data.toByteArray
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.world.ChunkPos
import java.io.File
import java.nio.channels.FileChannel

class WorldDataStorage(val world: World) {
    
    private val regionsFolder = File(world.worldFolder, "nova_region")
    private val regionFiles = HashMap<Long, RegionFile>()
    
    init {
        regionsFolder.mkdirs()
    }
    
    fun getRegion(pos: ChunkPos): RegionFile {
        val rx = pos.x shr 5
        val rz = pos.z shr 5
        
        val rid = (rx.toLong() shl 32) or rz.toLong()
        
        return regionFiles.getOrPut(rid) {
            val file = File(regionsFolder, "r.$rx.$rz.nvr")
            val regionFile = RegionFile(this, file, rx, rz)
            
            if (file.exists()) {
                file.inputStream().use {
                    val mbuf = it.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
                    val buf = Unpooled.wrappedBuffer(mbuf)
                    regionFile.read(buf)
                }
            }
            
            return@getOrPut regionFile
        }
    }
    
    fun saveAll() {
        regionFiles.removeIf { (_, regionFile) ->
            val buf = Unpooled.buffer()
            regionFile.write(buf)
            regionFile.file.writeBytes(buf.toByteArray())
            
            return@removeIf !regionFile.isAnyChunkLoaded()
        }
    }
    
}