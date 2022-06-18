package xyz.xenondevs.nova.data.world

import io.netty.buffer.Unpooled
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.util.data.*
import xyz.xenondevs.nova.util.getOrSet
import xyz.xenondevs.nova.world.ChunkPos
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.component1
import kotlin.collections.component2
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
internal class RegionFile(val world: WorldDataStorage, val file: File, val regionX: Int, val regionZ: Int) {
    
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
    
    //<editor-fold desc="Writing">
    
    fun save(chunk: RegionChunk) {
        val packedCoords = chunk.packedCoords
        var pos = chunkPositions[packedCoords.toInt()]?.get()
        if (pos != null) {
            if (!chunk.hasData()) {
                delete(chunk, pos)
                return
            }
            raf.seek(pos)
            val length = raf.readInt()
            val buf = Unpooled.buffer()
            val poolChanged = chunk.write(buf, typePool)
            val newData = buf.toByteArray()
            val newLength = newData.size
            raf.seek(pos)
            raf.writeInt(newLength)
            raf.append(pos + 4, pos + 4 + length, newData)
            adjustHeader(pos, (newLength - length).toLong(), poolChanged)
        } else if (chunk.hasData()) {
            raf.seek(raf.length())
            pos = raf.filePointer
            val buf = Unpooled.buffer()
            val poolChanged = chunk.write(buf, typePool)
            val newData = buf.toByteArray()
            chunkPositions[packedCoords.toInt()] = AtomicLong(pos)
            writeHeader(rewritePool = poolChanged)
            raf.writeInt(newData.size)
            raf.write(newData)
        }
    }
    
    private fun delete(chunk: RegionChunk, pos: Long) {
        raf.seek(pos)
        val length = raf.readInt() + 4
        chunkPositions.remove(chunk.packedCoords.toInt())
        raf.append(pos, pos + length, byteArrayOf())
        adjustHeader(pos, -length.toLong())
    }
    
    private fun adjustHeader(from: Long, offset: Long, rewritePool: Boolean = false) {
        chunkPositions.asSequence()
            .filter { it.value.get() > from }
            .forEach { (_, idx) -> idx.addAndGet(offset) }
        writeHeader(rewritePool)
    }
    
    private fun writeHeader(rewritePool: Boolean = false) {
        raf.seek(1)
        val currentTypeLength = raf.readInt()
        raf.skipBytes(currentTypeLength)
        val pos = raf.filePointer
        val currentSize = raf.readInt()
        val newSize = chunkPositions.size * 12
        var delta = (newSize - currentSize).toLong()
        val out = Unpooled.buffer()
        if (rewritePool) {
            val poolBytesCount = out.writeStringList(typePool)
            raf.seek(1)
            raf.writeInt(poolBytesCount)
            delta += (poolBytesCount - currentTypeLength).toLong()
        }
        out.writeInt(newSize)
        chunkPositions.forEach { (packed, pos) ->
            out.writeInt(packed)
            out.writeLong(pos.addAndGet(delta))
        }
        raf.append(if (rewritePool) 5 else pos, pos + 4 + currentSize, out.toByteArray())
    }
    
    fun saveAll() {
        if (CREATE_BACKUPS) {
            val ins = file.inputStream()
            val out = DataOutputStream(backupFile.outputStream())
            use(ins, out) {
                out.writeLong(file.length())
                ins.copyTo(out)
            }
        }
        
        chunks.forEachIndexed { idx, chunk ->
            if (chunk == null) return@forEachIndexed
            
            save(chunk)
            if (!chunk.pos.isLoaded()) chunks[idx] = null
        }
        
        if (CREATE_BACKUPS)
            backupFile.delete()
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Reading">
    
    private fun readHeader() {
        raf.seek(5)
        typePool.addAll(raf.readStringList())
        repeat(raf.readInt() / 12) {
            chunkPositions[raf.readInt()] = AtomicLong(raf.readLong())
        }
    }
    
    fun read(packedCoords: Int): RegionChunk {
        val chunk = RegionChunk(
            file = this,
            relChunkX = packedCoords shr 5,
            relChunkZ = packedCoords and 0x1F
        )
        val pos = chunkPositions[packedCoords]?.get() ?: return chunk
        raf.seek(pos)
        val bytes = ByteArray(raf.readInt())
        raf.read(bytes)
        chunk.read(Unpooled.wrappedBuffer(bytes), typePool)
        return chunk
    }
    
    //</editor-fold>
    
    fun getChunk(pos: ChunkPos): RegionChunk {
        val dx = pos.x and 0x1F
        val dz = pos.z and 0x1F
        val packedCoords = dx shl 5 or dz
        return chunks.getOrSet(packedCoords) { read(packedCoords) }
    }
    
}
