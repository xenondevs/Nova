package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.bukkit.World
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.util.data.use
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunkReader
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.*

private const val MAGIC: Int = 0xB7E21337.toInt()
private const val FILE_VERSION: Byte = 2
private const val NUM_CHUNKS = 1024
private val CREATE_BACKUPS by MAIN_CONFIG.entry<Boolean>("performance", "region_backups")

internal abstract class RegionizedFile<T : RegionizedChunk>(
    protected val file: File,
    protected val world: World,
    protected val regionX: Int, protected val regionZ: Int,
    private val _chunks: Array<T>
) {
    
    val chunks: List<T>
        get() = _chunks.asList()
    
    fun getChunk(pos: ChunkPos): T {
        val dx = pos.x and 0x1F
        val dz = pos.z and 0x1F
        val packedCoords = dx shl 5 or dz
        return _chunks[packedCoords]
    }
    
    suspend fun save() = coroutineScope {
        // serialize chunks
        val chunkBitmask = BitSet(NUM_CHUNKS)
        val chunksBuffer = ByteArrayOutputStream()
        val chunksWriter = ByteWriter.fromStream(chunksBuffer)
        for ((chunkIdx, chunk) in _chunks.withIndex()) {
            chunkBitmask.set(chunkIdx, chunk.write(chunksWriter))
        }
        
        withContext(Dispatchers.IO) {
            // create backup
            val backupFile = RegionizedFileReader.getBackupFile(file)
            if (CREATE_BACKUPS && file.exists())
                RegionizedFileReader.writeBackup(file, backupFile)
            
            // write region file
            file.outputStream().buffered().use { out ->
                val writer = ByteWriter.fromStream(out)
                writer.writeInt(MAGIC)
                writer.writeByte(FILE_VERSION)
                writer.writeBytes(Arrays.copyOf(chunkBitmask.toByteArray(), NUM_CHUNKS / 8))
                writer.writeBytes(chunksBuffer.toByteArray())
            }
            
            if (CREATE_BACKUPS && backupFile.exists())
                backupFile.delete()
        }
    }
    
}

internal abstract class RegionizedFileReader<C : RegionizedChunk, F : RegionizedFile<C>>(
    private val createArray: (Int, (Int) -> C) -> Array<C>,
    private val createFile: (File, World, Int, Int, Array<C>) -> F,
    private val chunkReader: RegionizedChunkReader<C>
) {
    
    fun read(file: File, world: World, regionX: Int, regionZ: Int): F {
        LOGGER.info("Loading region file $file")
        
        val backupFile = getBackupFile(file)
        try {
            // restore backup if present
            if (backupFile.exists())
                restoreBackup(file, backupFile)
            
            // read region file
            if (file.length() != 0L) {
                file.inputStream().buffered().use { inp ->
                    val reader = ByteReader.fromStream(inp)
                    
                    // verify magic and version
                    if (reader.readInt() != MAGIC)
                        throw IllegalStateException(file.absolutePath + " is not a valid region file")
                    val version = reader.readByte()
                    if (version != FILE_VERSION)
                        throw IllegalStateException(file.absolutePath + " has an invalid version (expected $FILE_VERSION, got $version)")
                    
                    // read chunks
                    val chunkBitmask = BitSet.valueOf(reader.readBytes(NUM_CHUNKS / 8))
                    val chunks = createArray(NUM_CHUNKS) {
                        val pos = chunkIdxToPos(it, world, regionX, regionZ)
                        if (chunkBitmask.get(it)) 
                            chunkReader.read(pos, reader) 
                        else chunkReader.createEmpty(pos)
                    }
                    
                    return createFile(file, world, regionX, regionZ, chunks)
                }
            } else {
                val chunks = createArray(NUM_CHUNKS) { chunkReader.createEmpty(chunkIdxToPos(it, world, regionX, regionZ)) }
                return createFile(file, world, regionX, regionZ, chunks)
            }
        } catch (t: Throwable) {
            throw IllegalStateException("Could not initialize region file $file", t)
        }
    }
    
    companion object {
        
        fun writeBackup(file: File, backupFile: File) {
            val ins = file.inputStream().buffered()
            val out = DataOutputStream(backupFile.outputStream().buffered())
            use(ins, out) {
                out.writeLong(file.length())
                ins.copyTo(out)
            }
        }
        
        fun restoreBackup(file: File, backupFile: File) {
            LOGGER.warning("Restoring region file $file from backup $backupFile")
            val ins = DataInputStream(backupFile.inputStream().buffered())
            val out = file.outputStream().buffered()
            
            use(ins, out) {
                val length = ins.readLong()
                if (length == backupFile.length() - 8) {
                    ins.copyTo(out)
                } else LOGGER.warning("Backup file $backupFile is corrupted")
            }
            backupFile.delete()
        }
        
        fun getBackupFile(file: File): File =
            File(file.parentFile, file.name + ".backup")
        
        fun chunkIdxToPos(idx: Int, world: World, regionX: Int, regionZ: Int): ChunkPos {
            val x = regionX shl 5 or (idx shr 5)
            val z = regionZ shl 5 or (idx and 0x1F)
            return ChunkPos(world.uid, x, z)
        }
        
    }
    
}