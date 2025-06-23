package xyz.xenondevs.nova.world.format

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap
import org.bukkit.World
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.io.byteWriter
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.CompressionType
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunkReader
import xyz.xenondevs.nova.world.format.legacy.LegacyRegionizedFileReader
import java.io.ByteArrayOutputStream
import java.util.*

private const val NUM_CHUNKS = 1024
private val COMPRESSION_TYPE by MAIN_CONFIG.entry<CompressionType>("world", "format", "compression")

internal abstract class RegionizedFile<T : RegionizedChunk>(
    private val magic: Int,
    private val fileVersion: Byte,
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
    
    fun save(): ByteArray {
        val compressionType = COMPRESSION_TYPE
        
        // serialize chunks
        val chunkBitmask = BitSet(NUM_CHUNKS)
        val chunksBuffer = ByteArrayOutputStream()
        compressionType.wrapOutput(chunksBuffer).use { compOut ->
            val chunksWriter = ByteWriter.fromStream(compOut)
            for ((chunkIdx, chunk) in _chunks.withIndex()) {
                chunkBitmask.set(chunkIdx, chunk.write(chunksWriter))
            }
        }
        
        // write region file
        val bin = byteWriter {
            writeInt(magic)
            writeByte(fileVersion)
            writeByte(compressionType.ordinal.toByte())
            writeBytes(chunkBitmask.toByteArray().copyOf(NUM_CHUNKS / 8))
            writeBytes(chunksBuffer.toByteArray())
        }
        
        return bin
    }
    
}

private const val LEGACY_MAGIC = 0xB7E21337.toInt()

internal abstract class RegionizedFileReader<C : RegionizedChunk, F : RegionizedFile<C>>(
    private val magic: Int,
    private val version: Byte,
    private val createArray: (Int, (Int) -> C) -> Array<C>,
    private val createFile: (Array<C>) -> F,
    private val chunkReader: RegionizedChunkReader<C>,
    vararg legacyReaders: Pair<Int, LegacyRegionizedFileReader<C, F>>
) {
    
    private val legacyReaders: Byte2ObjectMap<LegacyRegionizedFileReader<C, F>> =
        legacyReaders.associateTo(Byte2ObjectOpenHashMap()) { (version, reader) -> version.toByte() to reader }
    
    fun read(reader: ByteReader?, world: World, regionX: Int, regionZ: Int): F {
        if (reader != null) {
            // verify magic and version
            val fileMagic = reader.readInt()
            if (fileMagic != magic && fileMagic != LEGACY_MAGIC)
                throw IllegalStateException("Not a valid region file")
            
            // choose reader (legacy reader / readLatest) based on file version
            val fileVersion = reader.readByte()
            if (fileVersion == version) {
                return readLatest(reader, world, regionX, regionZ)
            } else {
                val legacyReader = legacyReaders.get(fileVersion)
                    ?: throw IllegalStateException("Unsupported region file version $fileVersion")
                return legacyReader.read(reader, world, regionX, regionZ)
            }
        } else {
            // create empty region file
            val chunks = createArray(NUM_CHUNKS) { chunkReader.createEmpty(chunkIdxToPos(it, world, regionX, regionZ)) }
            return createFile(chunks)
        }
    }
    
    private fun readLatest(reader: ByteReader, world: World, regionX: Int, regionZ: Int): F {
        // read chunks
        val compressionType = CompressionType.entries[reader.readByte().toInt()]
        compressionType.wrapInput(reader.asInputStream()).use { decompInp ->
            val chunkBitmask = BitSet.valueOf(reader.readBytes(NUM_CHUNKS / 8))
            val decompReader = ByteReader.fromStream(decompInp)
            val chunks = createArray(NUM_CHUNKS) {
                val pos = chunkIdxToPos(it, world, regionX, regionZ)
                if (chunkBitmask.get(it))
                    chunkReader.read(pos, decompReader)
                else chunkReader.createEmpty(pos)
            }
            
            return createFile(chunks)
        }
    }
    
    companion object {
        
        fun chunkIdxToPos(idx: Int, world: World, regionX: Int, regionZ: Int): ChunkPos {
            val x = regionX shl 5 or (idx shr 5)
            val z = regionZ shl 5 or (idx and 0x1F)
            return ChunkPos(world.uid, x, z)
        }
        
    }
    
}