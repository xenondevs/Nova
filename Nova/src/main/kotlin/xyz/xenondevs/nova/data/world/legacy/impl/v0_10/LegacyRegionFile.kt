package xyz.xenondevs.nova.data.world.legacy.impl.v0_10

import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.RegionChunk
import xyz.xenondevs.nova.data.world.WorldDataStorage
import xyz.xenondevs.nova.util.data.readStringListLegacy
import xyz.xenondevs.nova.util.data.use
import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.set

private val CREATE_BACKUPS by configReloadable { DEFAULT_CONFIG.getBoolean("performance.region_backups") }

/**
 * A binary region file.
 *
 * Format:
 * ```
 * RegionFile {
 *      int8     version;
 *      int32    type_pool_size;
 *      int32    type_pool_count;
 *      string[] type_pool[type_pool_count];
 *      int32    header_size;
 *      chunk_positions[header_size / 12]; {
 *          int32       packed_world_pos;
 *          Int64       pos;
 *      }
 *      chunk_data[n]; {
 *          int32      size;
 *          int16      packed_rel_pos;
 *          int32      type_pool_index;
 *          blockState state;
 *      }
 * }
 * ```
 * TODO
 * - remove unused types from pool
 * - Instead of storing chunk size, get distance to next chunk in file
 */
internal class LegacyRegionFile(val world: WorldDataStorage?, val file: File, val regionX: Int, val regionZ: Int) {
    
    constructor(file: File, regionX: Int, regionZ: Int) : this(null, file, regionX, regionZ)
    
    val chunks = arrayOfNulls<RegionChunk?>(1024)
    private val backupFile = File(file.parentFile, file.name + ".backup")
    
    /**
     * Position of chunk data in the file
     */
    private val chunkPositions = LinkedHashMap<Int, AtomicLong>()
    private val typePool = ArrayList<String>()
    private val raf: RandomAccessFile
    
    init {
        if (backupFile.exists()) {
            LOGGER.warning("Restoring region file $file from backup $backupFile")
            val ins = DataInputStream(backupFile.inputStream())
            val out = file.outputStream()
            
            use(ins, out) {
                val length = ins.readLong()
                if (length == backupFile.length() - 8) {
                    ins.copyTo(out)
                } else LOGGER.warning("Backup file $backupFile is corrupted")
            }
            backupFile.delete()
        }
        
        raf = RandomAccessFile(file, "rw")
    }
    
    fun init() {
        if (raf.length() == 0L) {
            raf.setLength(0L)
            raf.writeByte(0) // File format version
            raf.writeInt(4) // Type pool size (including count)
            raf.writeInt(0) // Type pool count
            raf.writeInt(0) // Header size
        } else {
            raf.seek(0)
            if (raf.readByte().toInt() != 0)
                throw IllegalStateException(file.absolutePath + " is not a valid region file")
            readHeader()
        }
    }
    
    fun close() {
        raf.close()
    }
    
    //<editor-fold desc="Reading">
    
    private fun readHeader() {
        raf.seek(5)
        typePool.addAll(raf.readStringListLegacy())
        repeat(raf.readInt() / 12) {
            chunkPositions[raf.readInt()] = AtomicLong(raf.readLong())
        }
    }
    
    fun read(packedCoords: Int): RegionChunk {
        val chunk = RegionChunk(
            regionX,
            regionZ,
            Bukkit.getWorlds()[0],
            relChunkX = packedCoords shr 5,
            relChunkZ = packedCoords and 0x1F
        )
        val pos = chunkPositions[packedCoords]?.get() ?: return chunk
        raf.seek(pos)
        val bytes = ByteArray(raf.readInt())
        raf.read(bytes)
        chunk.readLegacy(Unpooled.wrappedBuffer(bytes), typePool)
        chunks[packedCoords] = chunk
        return chunk
    }
    
    //</editor-fold>
    
    fun readAllChunks() {
        chunkPositions.keys.forEach(::read)
    }
}

