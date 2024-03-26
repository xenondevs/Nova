package xyz.xenondevs.nova.world.format

import org.bukkit.World
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.util.concurrent.isServerThread
import xyz.xenondevs.nova.util.data.use
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.*
import java.util.logging.Level

private const val MAGIC: Int = 0xB7E21337.toInt()
private const val FILE_VERSION: Byte = 2

private const val NUM_CHUNKS = 1024

private val CREATE_BACKUPS by MAIN_CONFIG.entry<Boolean>("performance", "region_backups")

internal class RegionFile(
    private val file: File,
    private val world: World,
    private val regionX: Int, private val regionZ: Int,
    private val _chunks: Array<RegionChunk> = Array(NUM_CHUNKS) { RegionChunk(chunkIdxToPos(it, world, regionX, regionZ)) }
) {
    
    val chunks: List<RegionChunk>
        get() = _chunks.asList()
    
    fun getChunk(pos: ChunkPos): RegionChunk {
        val dx = pos.x and 0x1F
        val dz = pos.z and 0x1F
        val packedCoords = dx shl 5 or dz
        return _chunks[packedCoords]
    }
    
    fun isAnyChunkEnabled(): Boolean {
        for (chunk in _chunks) {
            if (chunk.isEnabled.get())
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
    
    fun save() {
        val backupFile = getBackupFile(file)
        if (CREATE_BACKUPS && file.exists()) {
            val ins = file.inputStream().buffered()
            val out = DataOutputStream(backupFile.outputStream().buffered())
            use(ins, out) {
                out.writeLong(file.length())
                ins.copyTo(out)
            }
        }
        
        file.outputStream().buffered().use { out ->
            val writer = ByteWriter.fromStream(out)
            
            writer.writeInt(MAGIC)
            writer.writeByte(FILE_VERSION)
            
            val chunkBitmask = BitSet(NUM_CHUNKS)
            val chunksBuffer = ByteArrayOutputStream()
            val chunksWriter = ByteWriter.fromStream(chunksBuffer)
            for ((chunkIdx, chunk) in _chunks.withIndex()) {
                chunkBitmask.set(chunkIdx, chunk.write(chunksWriter))
            }
            
            writer.writeBytes(Arrays.copyOf(chunkBitmask.toByteArray(), NUM_CHUNKS / 8))
            writer.writeBytes(chunksBuffer.toByteArray())
        }
        
        if (CREATE_BACKUPS && backupFile.exists())
            backupFile.delete()
    }
    
    companion object {
        
        fun read(file: File, world: World, regionX: Int, regionZ: Int): RegionFile {
            if (IS_DEV_SERVER && isServerThread) {
                LOGGER.log(Level.WARNING, "Loading region file $file on main thread. This should be avoided.", Throwable())
            }
            
            LOGGER.info("Loading region file $file")
            
            val backupFile = getBackupFile(file)
            try {
                // restore backup if present
                if (backupFile.exists())
                    restoreBackup(file, backupFile)
                
                // read region file
                if (file.length() != 0L) {
                    val reader = ByteReader.fromStream(file.inputStream().buffered())
                    
                    // verify magic and version
                    if (reader.readInt() != MAGIC)
                        throw IllegalStateException(file.absolutePath + " is not a valid region file")
                    val version = reader.readByte()
                    if (version != FILE_VERSION)
                        throw IllegalStateException(file.absolutePath + " has an invalid version (expected $FILE_VERSION, got $version)")
                    
                    // read chunks
                    val chunkBitmask = BitSet.valueOf(reader.readBytes(NUM_CHUNKS / 8))
                    val chunks = Array(NUM_CHUNKS) {
                        val pos = chunkIdxToPos(it, world, regionX, regionZ)
                        if (chunkBitmask.get(it)) RegionChunk.read(pos, reader) else RegionChunk(pos)
                    }
                    
                    return RegionFile(file, world, regionX, regionZ, chunks)
                } else return RegionFile(file, world, regionX, regionZ)
            } catch (t: Throwable) {
                throw IllegalStateException("Could not initialize region file $file", t)
            }
        }
        
        private fun restoreBackup(file: File, backupFile: File) {
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
        
        private fun getBackupFile(file: File): File =
            File(file.parentFile, file.name + ".backup")
        
        private fun chunkIdxToPos(idx: Int, world: World, regionX: Int, regionZ: Int): ChunkPos {
            val x = regionX shl 5 or (idx shr 5)
            val z = regionZ shl 5 or (idx and 0x1F)
            return ChunkPos(world.uid, x, z)
        }
        
    }
    
}